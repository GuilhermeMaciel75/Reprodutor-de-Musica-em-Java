import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

public class Player {

    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice where audio samples are written to.
     */
    private AudioDevice device;

    private PlayerWindow window;

    private SwingWorker runner;

    //Variável booleana responsável por saber em qual estado o botão de play/ pause está
    private boolean activeButtonPlayPause = true;

    // Variável que é responsável por verificar se o botão de pause está ativo
    private boolean pressButtonPlayPause = true;
    // Variável que é responsável por verificar se o botão de STOP está ativo, caso ele esteja  a música para por completo
    private boolean activeButtonStop = false;
    private boolean playing = true;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition lockCondition = lock.newCondition();


    //Array dinamicos, o primeiro salva todas as músicas da classe Song
    //Já o segundo guarda apenas as musicas que serão coladas no display para vizualização pelo usuário
    private ArrayList<Song> songsListDynamic = new ArrayList<Song>();
    private ArrayList<String[]> musicsListDynamic = new ArrayList<String[]>();
    private String[][] musicsListStatic = {};

    //Variaveis utilizadas nas funções
    private Song songPlaying; // Variável do tipo song, que armazena as informações da música que está sendo tocada no momento
    private Song removeMusic; // Variável do do tipo song que armazena as informaçõs da música que foi removida da lista de reprodução
    private int idxMusic; // Variável quer armazena o ínidice atual da música que esta sendo tocada ou Da música que será removida

    public int currentFrame = 0;

    public void musicPlaying(){

        Thread running = new Thread(() -> {
            playing = true;
            while(playing && pressButtonPlayPause){
                try{
                    window.setTime((int) (currentFrame * (int) songPlaying.getMsPerFrame()), (int) songPlaying.getMsLength());
                    if(window.getScrubberValue() < songPlaying.getMsLength()) {
                        playing = playNextFrame();
                    }
                    else{
                        playing = false;
                    }
                } catch (JavaLayerException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        running.start();
    }

    //Função responsável por reproduzir a múscia
    private final ActionListener buttonListenerPlayNow = e -> {
        currentFrame = 0;

        Thread playing = new Thread(()->{
            try{
                lock.lock();

                //Pegando a música especificada pelo usuário (Aquela que ele clicou)
                idxMusic = window.getIndex();
                songPlaying = songsListDynamic.get(idxMusic);

                window.setPlayingSongInfo(songPlaying.getTitle(), songPlaying.getAlbum(), songPlaying.getArtist()); //Setando as informações na tela
                pressButtonPlayPause = true;
                window.setPlayPauseButtonIcon(1);
                window.setEnabledPlayPauseButton(true);
                window.setEnabledStopButton(true);
                activeButtonPlayPause = true;
                activeButtonStop = true;

                try {
                    device = FactoryRegistry.systemRegistry().createAudioDevice();
                    device.open(decoder = new Decoder());
                    bitstream = new Bitstream(songPlaying.getBufferedInputStream());
                    musicPlaying();

                } catch (JavaLayerException | FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }

            }
            finally {
                lock.unlock();
            }

        });

        playing.start();
    };
    private final ActionListener buttonListenerRemove = e -> {

        //Pegando a música que foi selecionada pelo usuário
        idxMusic = window.getIndex();
        removeMusic = songsListDynamic.get(idxMusic);

        //Removendo a música da lista mostrada ao usuário
        musicsListDynamic.remove(idxMusic);
        musicsListStatic = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
        window.setQueueList(musicsListStatic);

        //Removendo a música da lista de songs
        songsListDynamic.remove(idxMusic);

        //Condicional que verifica se a música que está tocando é a que foi removida
        if(currentFrame != 0 && songPlaying == removeMusic){
            stop();

        }


    };
    private final ActionListener buttonListenerAddSong = e -> {

        Song music;

        //Abertura da aba para recebimento do arquivo de música do usuário
        try {
            music = this.window.openFileChooser();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (BitstreamException ex) {
            throw new RuntimeException(ex);
        } catch (UnsupportedTagException ex) {
            throw new RuntimeException(ex);
        } catch (InvalidDataException ex) {
            throw new RuntimeException(ex);
        }


        //Adicionando a nova música ao array de display
        musicsListDynamic.add(music.getDisplayInfo());
        musicsListStatic = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
        window.setQueueList(musicsListStatic);

        //Adicionando a nova música ao array de song (Array, que efetivamente executa a música)
        songsListDynamic.add(music);

    };
    private final ActionListener buttonListenerPlayPause = e -> {

        //Caso o botão pause esteja ativo (Música Em execução)
        if (activeButtonPlayPause == true){
            pressButtonPlayPause = false;
            activeButtonPlayPause = false;
            window.setPlayPauseButtonIcon(0);
        }

        //Caso o botão esteja no estado de play (Música pausada)
        else{
            pressButtonPlayPause = true;
            activeButtonPlayPause = true;
            musicPlaying();
            window.setPlayPauseButtonIcon(1);
        }

    };
    private final ActionListener buttonListenerStop = e -> {

        if(activeButtonStop == true){
            stop();
        }

    };
    private final ActionListener buttonListenerNext = e -> {};
    private final ActionListener buttonListenerPrevious = e -> {};
    private final ActionListener buttonListenerShuffle = e -> {};
    private final ActionListener buttonListenerLoop = e -> {};
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }
    };

    public Player() {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                ("Spotfy"),
                musicsListStatic,
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter)
        );
    }


    //<editor-fold desc="Essential">

    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
            currentFrame++;
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        // TODO Is this thread safe?
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException Generic Bitstream exception.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > currentFrame) {
            int framesToSkip = newFrame - currentFrame;
            boolean condition = true;
            while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
        }
    }

    private void stop(){
        pressButtonPlayPause = false;
        window.setEnabledStopButton(false);
        window.resetMiniPlayer();
    }

    //</editor-fold>
}
