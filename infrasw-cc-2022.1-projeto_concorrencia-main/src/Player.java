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

    private Boolean active_play_pause = new Boolean(true);
    private Boolean press_play_pause = new Boolean(true);
    private Boolean active_stop = new Boolean(false);


    private final ReentrantLock lock = new ReentrantLock();
    private final Condition lockCondition = lock.newCondition();


    //Array dinamicos
    private ArrayList<Song> lista_songs = new ArrayList<Song>();
    private ArrayList<String[]> musics_temp = new ArrayList<String[]>();
    private String[][] musics = {};

    //Variaveis utilizadas nas funções
    private Song actual_song;
    private Song remove_music;
    private int idx;

    public int currentFrame = 0;

    //Função responsável por reproduzir a múscia
    private final ActionListener buttonListenerPlayNow = e -> {
        currentFrame = 0;

        //Pegando a música especificada pelo usuário (Aquela que ele clicou)
        idx = window.getIndex();
        actual_song = lista_songs.get(idx);
        press_play_pause = true;

        //Criando a Thread de execução da música
        runner = new SwingWorker(){
            @Override
            public Object doInBackground() throws Exception{

                window.setPlayingSongInfo(actual_song.getTitle(), actual_song.getAlbum(), actual_song.getArtist()); //Setando as informações na tela

                //Condicional responsável por fazer a música executar até os bitstreans terminarem, quando eles terminam
                //A música é finalizada
                if(bitstream != null){
                    try {
                        bitstream.close();
                    } catch (BitstreamException ex) {
                        throw new RuntimeException(ex);
                    }

                    device.close();
                }

                //Declaração do device e bitstream
                try {
                    device = FactoryRegistry.systemRegistry().createAudioDevice();
                    device.open(decoder = new Decoder());
                    bitstream = new Bitstream(actual_song.getBufferedInputStream());
                } catch (JavaLayerException | FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }

                //Loop de repetição que coloca o tempo na tela, faz a música efetivamente rodar
                //Verifica se o botao_play_pause foi clicado
                while(true){
                    if (press_play_pause){
                        try {
                            window.setTime((int) (currentFrame * (int) actual_song.getMsPerFrame()), (int) actual_song.getMsLength());
                            window.setPlayPauseButtonIcon(1);
                            window.setEnabledPlayPauseButton(true);
                            window.setEnabledStopButton(true);
                            active_play_pause = true;
                            active_stop = true;

                            playNextFrame();

                        } catch (JavaLayerException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                }

            }

        };

        runner.execute();

    };
    private final ActionListener buttonListenerRemove = e -> {

        //Pegando a música que foi selecionada pelo usuário
        idx = window.getIndex();
        remove_music = lista_songs.get(idx);

        //Removendo a música da lista mostrada ao usuário
        musics_temp.remove(idx);
        musics = musics_temp.toArray(new String[this.musics_temp.size()][7]);
        window.setQueueList(musics);

        //Removendo a música da lista de songs
        lista_songs.remove(idx);

        //Condicional que verifica se a música que está tocando é a que foi removida
        if(currentFrame != 0 && actual_song == remove_music){
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
        musics_temp.add(music.getDisplayInfo());
        musics = musics_temp.toArray(new String[this.musics_temp.size()][7]);
        window.setQueueList(musics);

        //Adicionando a nova música ao array de song (Array, que efetivamente executa a música)
        lista_songs.add(music);

    };
    private final ActionListener buttonListenerPlayPause = e -> {

        //Caso o botão pause esteja ativo (Música Em execução)
        if (active_play_pause == true){
            press_play_pause = false;
            active_play_pause = false;
            window.setPlayPauseButtonIcon(0);
        }

        //Caso o botão esteja no estado de play (Música pausada)
        else{
            press_play_pause = true;
            active_play_pause = true;
            window.setPlayPauseButtonIcon(1);
        }

    };
    private final ActionListener buttonListenerStop = e -> {

        if(active_stop == true){
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
                musics,
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
        press_play_pause = false;
        window.setEnabledStopButton(false);
        window.resetMiniPlayer();
    }
    //</editor-fold>
}
