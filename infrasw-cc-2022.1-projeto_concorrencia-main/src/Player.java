import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition lockCondition = lock.newCondition();

    private String[][] musics = new String[1][0];
    private Song[] lista_songs = new Song[1];
    int stopPlayNow = 0;

    private int currentFrame = 0;

    private final ActionListener buttonListenerPlayNow = e -> {
        currentFrame = 0;
        this.window.setPlayingSongInfo(lista_songs[0].getTitle(), lista_songs[0].getAlbum(), lista_songs[0].getArtist());

        if(this.bitstream != null){
            try {
                bitstream.close();
            } catch (BitstreamException ex) {
                throw new RuntimeException(ex);
            }

            device.close();
        }

        try {
            this.device = FactoryRegistry.systemRegistry().createAudioDevice();
        } catch (JavaLayerException ex) {
            throw new RuntimeException(ex);
        }
        try {
            this.device.open(this.decoder = new Decoder());
        } catch (JavaLayerException ex) {
            throw new RuntimeException(ex);
        }
        try {
            this.bitstream = new Bitstream(lista_songs[0].getBufferedInputStream());
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        while(true){
            //this.window.setTime(, lista_songs[0].getMsLength());
            try {
                playNextFrame();
            } catch (JavaLayerException ex) {
                throw new RuntimeException(ex);
            }
        }
    };
    private final ActionListener buttonListenerRemove = e -> {};
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

        musics[0] = music.getDisplayInfo();

        this.window.setQueueList(musics);

        lista_songs[0] = music;



    };
    private final ActionListener buttonListenerPlayPause = e -> {};
    private final ActionListener buttonListenerStop = e -> {};
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
