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
    private boolean playing = false;
    private boolean doubleMusic = false;
    private boolean musicRunning = true;

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

    private int idxMusicRemove;

    public int currentFrame = 0;

    //Função responsável por reproduzir a múscia

    private final ActionListener buttonListenerPlayNow = e -> {
        idxMusic = window.getIndex();
        musicRunner(idxMusic);
    };
    private final ActionListener buttonListenerRemove = e -> {

        //Pegando a música que foi selecionada pelo usuário
        idxMusicRemove = window.getIndex();
        removeMusic = songsListDynamic.get(idxMusicRemove);

        //Removendo a música da lista mostrada ao usuário
        musicsListDynamic.remove(idxMusicRemove);
        musicsListStatic = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
        window.setQueueList(musicsListStatic);

        //Removendo a música da lista de songs
        songsListDynamic.remove(idxMusicRemove);

        if (idxMusic < idxMusicRemove){
            idxMusic --;
        }

        else if(idxMusic == idxMusicRemove){
            stop();
        }

        /*
        //Condicional que verifica se a música que está tocando é a que foi removida
        if(currentFrame != 0 && songPlaying == removeMusic){
            stop();

        }
           */

    };
    private final ActionListener buttonListenerAddSong = e -> {
        Thread addSong = new Thread(() -> {
            Song music;

            //Abertura da aba para recebimento do arquivo de música do usuário
            try {
                lock.lock();
                music = this.window.openFileChooser();
                //Adicionando a nova música ao array de display
                musicsListDynamic.add(music.getDisplayInfo());
                musicsListStatic = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
                window.setQueueList(musicsListStatic);

                //Adicionando a nova música ao array de song (Array, que efetivamente executa a música)
                songsListDynamic.add(music);

            } catch (IOException | BitstreamException | UnsupportedTagException | InvalidDataException ex) {
                throw new RuntimeException(ex);
            }finally {
                lock.unlock();
            }

        });

        addSong.start();

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
    private final ActionListener buttonListenerNext = e -> {

        nextSong();
    };
    private final ActionListener buttonListenerPrevious = e -> {

        previousSong();
    };
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
        playing = false;
        pressButtonPlayPause = false;
        window.resetMiniPlayer();

    }

    public void musicPlaying(){

        Thread running = new Thread(() -> {
            musicRunning = true;
            while(musicRunning && pressButtonPlayPause) {
                try {

                    if (doubleMusic) {
                        doubleMusic = false;
                        break;
                    }
                    window.setTime((int) (currentFrame * (int) songPlaying.getMsPerFrame()), (int) songPlaying.getMsLength());
                    if (window.getScrubberValue() < songPlaying.getMsLength()) {

                        musicRunning = playNextFrame();

                    } else {

                        musicRunning = false;
                    }

                    if(!pressButtonPlayPause) {
                        break;
                    }

                    if(songsListDynamic.size() == 1){
                        window.setEnabledNextButton(false);
                        window.setEnabledPreviousButton(false);
                    }

                    else if(songsListDynamic.size() > 1 && idxMusic == 0) {
                        window.setEnabledNextButton(true);
                        window.setEnabledPreviousButton(false);

                    }

                    else if(songsListDynamic.size() > idxMusic + 1 && idxMusic > 0){
                        window.setEnabledNextButton(true);
                        window.setEnabledPreviousButton(true);
                    }

                    else if(idxMusic == songsListDynamic.size() - 1){
                        window.setEnabledNextButton(false);
                        window.setEnabledPreviousButton(true);
                    }

                } catch (JavaLayerException e) {
                    throw new RuntimeException(e);
                }
            }

            if(!musicRunning){
                playing = false;
            }

            if(playing == false && pressButtonPlayPause == true && idxMusic + 1 < musicsListDynamic.size()){
                nextSong();
            }

            else if(idxMusic + 1 > musicsListDynamic.size()) {
                stop();
            }


        });

        running.start();
    }

    public void musicRunner(int idxMusic){
        currentFrame = 0;
        //pressButtonPlayPause = true;

        if(playing){
            doubleMusic = true;
        }

        Thread playing = new Thread(()->{
            try{
                lock.lock();
                this.playing = true;

                //Pegando a música especificada pelo usuário (Aquela que ele clicou)
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
    }


    public void nextSong(){
        if(idxMusic + 1 < musicsListDynamic.size()){
            idxMusic ++;
            musicRunner(idxMusic);
        }
    }

    public  void previousSong(){
        if(idxMusic - 1 >= 0){
            idxMusic --;
            musicRunner(idxMusic);
        }
    }

    //</editor-fold>
}
