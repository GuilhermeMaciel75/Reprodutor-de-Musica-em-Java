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

    private String name;
    private Song actual_song;
    private Song remove_music;
    private int idx;

    public int currentFrame = 0;

    //Função responsável por reproduzir a múscia
    private final ActionListener buttonListenerPlayNow = e -> {
        currentFrame = 0;

        idx = window.getIndex();
        actual_song = lista_songs.get(idx);

        System.out.println(idx);
        press_play_pause = true;
        runner = new SwingWorker(){
            @Override
            public Object doInBackground() throws Exception{

                window.setPlayingSongInfo(actual_song.getTitle(), actual_song.getAlbum(), actual_song.getArtist());

                if(bitstream != null){
                    try {
                        bitstream.close();
                    } catch (BitstreamException ex) {
                        throw new RuntimeException(ex);
                    }

                    device.close();
                }

                try {
                    device = FactoryRegistry.systemRegistry().createAudioDevice();
                    device.open(decoder = new Decoder());
                    bitstream = new Bitstream(actual_song.getBufferedInputStream());
                } catch (JavaLayerException | FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }

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
        idx = window.getIndex();
        remove_music = lista_songs.get(idx);
        musics_temp.remove(idx);
        musics = musics_temp.toArray(new String[this.musics_temp.size()][7]);
        window.setQueueList(musics);
        lista_songs.remove(idx);

        if(currentFrame != 0 && actual_song == remove_music){
            press_play_pause = false;
            window.setEnabledStopButton(false);
            window.resetMiniPlayer();
        }


    };
    private final ActionListener buttonListenerAddSong = e -> {

        Song music;

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


        //array dinamico Lista Display
        musics_temp.add(music.getDisplayInfo());
        musics = musics_temp.toArray(new String[this.musics_temp.size()][7]);
        window.setQueueList(musics);

        //Array dinâmico Lista Songs
        lista_songs.add(music);

    };
    private final ActionListener buttonListenerPlayPause = e -> {

        if (active_play_pause == true){
            press_play_pause = false;
            active_play_pause = false;
            window.setPlayPauseButtonIcon(0);
        }

        else{
            press_play_pause = true;
            active_play_pause = true;
            window.setPlayPauseButtonIcon(1);
        }

    };
    private final ActionListener buttonListenerStop = e -> {

        if(active_stop == true){
            press_play_pause = false;
            window.setEnabledStopButton(false);
            window.resetMiniPlayer();
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
    //</editor-fold>
}
