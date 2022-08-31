package support;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Song {
    private final String uuid;
    private final String title;
    private final String album;
    private final String artist;
    private final String year;
    private final float msLength;
    private final String strLength;
    private final String filePath;
    private final int fileSize;
    private final int numFrames;
    private final float msPerFrame;

    /**
     * Creates a copy of the Song object passed in.
     * This method is NOT THREAD SAFE!
     *
     * @param song Song object to copy.
     */
    public Song(Song song) {
        this.uuid = song.getUuid();
        this.title = song.getTitle();
        this.album = song.getAlbum();
        this.artist = song.getArtist();
        this.year = song.getYear();
        this.strLength = song.getStrLength();
        this.msLength = song.getMsLength();
        filePath = song.getFilePath();
        fileSize = song.getFileSize();
        numFrames = song.getNumFrames();
        msPerFrame = song.getMsPerFrame();
    }

    /**
     * @param title      Song title.
     * @param album      Song album.
     * @param artist     Song artist.
     * @param year       Song year.
     * @param strLength  Length as String in the format 00:00:00
     * @param msLength   Length in milliseconds.
     * @param filePath   File path.
     * @param fileSize   File size in bytes.
     * @param numFrames  Number of MP3 frames.
     * @param msPerFrame Number of milliseconds per MP3 frame.
     */
    public Song(String uuid, String title, String album, String artist, String year, String strLength, float msLength, String filePath, int fileSize, int numFrames, float msPerFrame) {
        this.uuid = uuid;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.year = year;
        this.strLength = strLength;
        this.msLength = msLength;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.numFrames = numFrames;
        this.msPerFrame = msPerFrame;
    }

    /**
     * Converts the Song object passed in to a String array.
     * This method is NOT THREAD SAFE!
     *
     * @return String array with info to be displayed.
     */
    public String[] getDisplayInfo() {
        String[] copy = new String[6];
        copy[0] = this.getTitle();
        copy[1] = this.getAlbum();
        copy[2] = this.getArtist();
        copy[3] = this.getYear();
        copy[4] = this.getStrLength();
        copy[5] = this.getUuid();
        return copy;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getYear() {
        return year;
    }

    public float getMsLength() {
        return msLength;
    }

    public String getStrLength() {
        return strLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public float getMsPerFrame() {
        return msPerFrame;
    }

    public BufferedInputStream getBufferedInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(this.getFilePath()));
    }
}
