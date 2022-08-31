package support;

import com.formdev.flatlaf.FlatLightLaf;
import com.mpatric.mp3agic.*;
import javazoom.jl.decoder.BitstreamException;
import support.resources.fonts.roboto_condensed.Roboto;
import support.resources.icons.Icons24;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public class PlayerWindow {
    private final String[] columnTitles = new String[]{"Title", "Album", "Artist", "Year", "Length", "Path"};
    public final int BUTTON_ICON_PLAY = 0;
    public final int BUTTON_ICON_PAUSE = 1;

    private final JFrame window = new JFrame();
    private final JPanel queuePanel;
    private final JTable queueList;
    private final JButton playNowButton;
    private final JButton removeSongButton;
    private final JButton addSongButton;

    private final JPanel miniPlayerPanel;
    private final JLabel miniPlayerSongInfo;
    private final JLabel miniPlayerCurrentTime;
    private final JSlider miniPlayerScrubber;
    private final JLabel miniPlayerTotalTime;
    private final JToggleButton miniPlayerShuffleButton;
    private final JButton miniPlayerPreviousButton;
    private final JButton miniPlayerPlayPauseButton;
    private final JButton miniPlayerStopButton;
    private final JButton miniPlayerNextButton;
    private final JToggleButton miniPlayerLoopButton;

    /**
     * @param windowTitle               String to be used as the window title.
     * @param queueArray                String[][] with the queue. The array should contain in each position one array
     * @param buttonListenerPlayNow     ActionListener for the "Play Now" button.
     * @param buttonListenerRemove      ActionListener for the "Remove" button.
     * @param buttonListenerAddSong     ActionListener for the "Add Song" button.
     * @param buttonListenerShuffle     ActionListener for the "Shuffle" button.
     * @param buttonListenerPrevious    ActionListener for the "Previous" button.
     * @param buttonListenerPlayPause   ActionListener for the "Play/Pause" button.
     * @param buttonListenerStop        ActionListener for the "Stop" button.
     * @param buttonListenerNext        ActionListener for the "Next" button.
     * @param buttonListenerLoop      ActionListener for the "Loop" button.
     * @param scrubberMouseInputAdapter MouseInputAdapter for the Scrubber.
     */
    public PlayerWindow(
            String windowTitle,
            String[][] queueArray,
            ActionListener buttonListenerPlayNow,
            ActionListener buttonListenerRemove,
            ActionListener buttonListenerAddSong,
            ActionListener buttonListenerShuffle,
            ActionListener buttonListenerPrevious,
            ActionListener buttonListenerPlayPause,
            ActionListener buttonListenerStop,
            ActionListener buttonListenerNext,
            ActionListener buttonListenerLoop,
            MouseInputAdapter scrubberMouseInputAdapter) {

        // Setting theme and typeface.
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());

            Font baseFont = Font.createFont(
                    Font.TRUETYPE_FONT,
                    Objects.requireNonNull(Roboto.class.getResourceAsStream("RobotoCondensed-Regular.ttf")));
            Font finalFont = baseFont.deriveFont(Font.PLAIN, 14);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(finalFont);

            UIManager.put("Button.font", finalFont);
            UIManager.put("ToggleButton.font", finalFont);
            UIManager.put("RadioButton.font", finalFont);
            UIManager.put("CheckBox.font", finalFont);
            UIManager.put("ColorChooser.font", finalFont);
            UIManager.put("ComboBox.font", finalFont);
            UIManager.put("Label.font", finalFont);
            UIManager.put("List.font", finalFont);
            UIManager.put("MenuBar.font", finalFont);
            UIManager.put("MenuItem.font", finalFont);
            UIManager.put("RadioButtonMenuItem.font", finalFont);
            UIManager.put("CheckBoxMenuItem.font", finalFont);
            UIManager.put("Menu.font", finalFont);
            UIManager.put("PopupMenu.font", finalFont);
            UIManager.put("OptionPane.font", finalFont);
            UIManager.put("Panel.font", finalFont);
            UIManager.put("ProgressBar.font", finalFont);
            UIManager.put("ScrollPane.font", finalFont);
            UIManager.put("Viewport.font", finalFont);
            UIManager.put("TabbedPane.font", finalFont);
            UIManager.put("Table.font", finalFont);
            UIManager.put("TableHeader.font", finalFont);
            UIManager.put("TextField.font", finalFont);
            UIManager.put("FormattedTextField.font", finalFont);
            UIManager.put("PasswordField.font", finalFont);
            UIManager.put("TextArea.font", finalFont);
            UIManager.put("TextPane.font", finalFont);
            UIManager.put("EditorPane.font", finalFont);
            UIManager.put("TitledBorder.font", finalFont);
            UIManager.put("ToolBar.font", finalFont);
            UIManager.put("ToolTip.font", finalFont);
            UIManager.put("Tree.font", finalFont);
        } catch (IOException | FontFormatException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JPanel mainPanel = new JPanel();

        //<editor-fold desc="Queue Panel">
        queuePanel = new JPanel();

        JPanel queuePanelButtons = new JPanel();
        JScrollPane queueListPane = new JScrollPane();

        queueList = new JTable();

        queuePanel.setLayout(new BorderLayout());
        queueListPane.setViewportView(queueList);
        setQueueList(queueArray);
        queuePanelButtons.setLayout(new BoxLayout(queuePanelButtons, BoxLayout.X_AXIS));
        queuePanelButtons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        playNowButton = new JButton("Play Now");
        removeSongButton = new JButton("Remove");
        addSongButton = new JButton("Add song...");
        queuePanelButtons.add(playNowButton);
        queuePanelButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        queuePanelButtons.add(removeSongButton);
        queuePanelButtons.add(Box.createHorizontalGlue());
        queuePanelButtons.add(addSongButton);
        playNowButton.setEnabled(false);
        removeSongButton.setEnabled(false);
        queuePanel.add(queueListPane, BorderLayout.CENTER);
        queuePanel.add(queuePanelButtons, BorderLayout.PAGE_END);

        playNowButton.addActionListener(buttonListenerPlayNow);
        removeSongButton.addActionListener(buttonListenerRemove);
        addSongButton.addActionListener(buttonListenerAddSong);
        //</editor-fold>

        //<editor-fold desc="Mini-player Panel">
        miniPlayerPanel = new JPanel();
        JPanel miniPlayerInfoAndScrubber = new JPanel();
        JPanel miniPlayerScrubberPanel = new JPanel();
        JPanel miniPlayerButtons = new JPanel();

        miniPlayerSongInfo = new JLabel();
        miniPlayerCurrentTime = new JLabel("- - : - -");
        miniPlayerScrubber = new JSlider();
        miniPlayerTotalTime = new JLabel("- - : - -");
        miniPlayerShuffleButton = new JToggleButton(Icons24.shuffle);
        miniPlayerPreviousButton = new JButton(Icons24.previous);
        miniPlayerPlayPauseButton = new JButton(Icons24.play);
        miniPlayerStopButton = new JButton(Icons24.stop);
        miniPlayerNextButton = new JButton(Icons24.next);
        miniPlayerLoopButton = new JToggleButton(Icons24.loop);

        miniPlayerPanel.setLayout(new BoxLayout(miniPlayerPanel, BoxLayout.PAGE_AXIS));
        miniPlayerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        miniPlayerInfoAndScrubber.setLayout(new GridLayout(2, 1, 0, 0));
        GroupLayout groupLayout = new GroupLayout(miniPlayerScrubberPanel);
        miniPlayerScrubberPanel.setLayout(groupLayout);
        miniPlayerScrubber.setMaximum(0);
        miniPlayerScrubber.setEnabled(false);
        groupLayout.setHorizontalGroup(
                groupLayout.createSequentialGroup()
                        .addComponent(miniPlayerCurrentTime)
                        .addComponent(miniPlayerScrubber)
                        .addComponent(miniPlayerTotalTime)
        );
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup()
                        .addComponent(miniPlayerCurrentTime)
                        .addComponent(miniPlayerScrubber)
                        .addComponent(miniPlayerTotalTime)
        );
        miniPlayerInfoAndScrubber.add(miniPlayerSongInfo);
        miniPlayerInfoAndScrubber.add(miniPlayerScrubberPanel);

        miniPlayerButtons.setLayout(new BoxLayout(miniPlayerButtons, BoxLayout.X_AXIS));
        miniPlayerButtons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        miniPlayerShuffleButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerPreviousButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerPlayPauseButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerStopButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerNextButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerLoopButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerShuffleButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerPreviousButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerPlayPauseButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerStopButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerNextButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerLoopButton.setMaximumSize(new Dimension(35, 35));

        miniPlayerButtons.add(Box.createHorizontalGlue());
        miniPlayerButtons.add(miniPlayerShuffleButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerPreviousButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerPlayPauseButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerStopButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerNextButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerLoopButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(Box.createHorizontalGlue());

        miniPlayerShuffleButton.setEnabled(false);
        miniPlayerPreviousButton.setEnabled(false);
        miniPlayerPlayPauseButton.setEnabled(false);
        miniPlayerStopButton.setEnabled(false);
        miniPlayerNextButton.setEnabled(false);
        miniPlayerLoopButton.setEnabled(false);

        miniPlayerPanel.add(miniPlayerInfoAndScrubber);
        miniPlayerPanel.add(miniPlayerButtons);

        miniPlayerShuffleButton.addActionListener(buttonListenerShuffle);
        miniPlayerPreviousButton.addActionListener(buttonListenerPrevious);
        miniPlayerPlayPauseButton.addActionListener(buttonListenerPlayPause);
        miniPlayerStopButton.addActionListener(buttonListenerStop);
        miniPlayerNextButton.addActionListener(buttonListenerNext);
        miniPlayerLoopButton.addActionListener(buttonListenerLoop);
        miniPlayerScrubber.addMouseMotionListener(scrubberMouseInputAdapter);
        miniPlayerScrubber.addMouseListener(scrubberMouseInputAdapter);
        //</editor-fold>

        window.setLayout(new BorderLayout());
        window.setTitle(windowTitle);
        // Unreliable behavior with setMinimumSize. Apparently when using high dpi screens with fractional scaling.
        // window.setMinimumSize(new Dimension(667, 450));
        window.setSize(718, 600);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(queuePanel);
        mainPanel.add(miniPlayerPanel);
        window.add(mainPanel);
        window.setVisible(true);
    }

    /**
     * Sets the information to be displayed in the queue list. Should be called whenever a song is added or removed.
     *
     * @param queueArray String[][] with the queue. The array should contain in each position one Song converted to array.
     */
    public void setQueueList(String[][] queueArray) {
        queueList.setShowHorizontalLines(true);
        queueList.setDragEnabled(false);
        queueList.setColumnSelectionAllowed(false);
        queueList.getTableHeader().setReorderingAllowed(false);
        queueList.getTableHeader().setResizingAllowed(false);
        queueList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        queueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queueList.setModel(new DefaultTableModel(queueArray, columnTitles) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        queueList.getSelectionModel().addListSelectionListener(e -> {
            if (queueList.getSelectionModel().isSelectionEmpty()) {
                playNowButton.setEnabled(false);
                removeSongButton.setEnabled(false);
            } else {
                playNowButton.setEnabled(true);
                removeSongButton.setEnabled(true);
            }
        });
        ((DefaultTableCellRenderer) queueList.getTableHeader().getDefaultRenderer())
                .setHorizontalAlignment(JLabel.LEFT);
        queueList.getColumnModel().getColumn(0).setMinWidth(180);
        queueList.getColumnModel().getColumn(1).setMinWidth(180);
        queueList.getColumnModel().getColumn(2).setMinWidth(180);
        queueList.getColumnModel().getColumn(3).setMinWidth(70);
        queueList.getColumnModel().getColumn(4).setMinWidth(60);
        queueList.getColumnModel().getColumn(5).setMinWidth(0);
        queueList.getColumnModel().getColumn(0).setPreferredWidth(180);
        queueList.getColumnModel().getColumn(1).setPreferredWidth(180);
        queueList.getColumnModel().getColumn(2).setPreferredWidth(180);
        queueList.getColumnModel().getColumn(3).setPreferredWidth(70);
        queueList.getColumnModel().getColumn(4).setPreferredWidth(60);
        queueList.getColumnModel().getColumn(5).setPreferredWidth(0);
    }

    /**
     * Sets the information displayed on the mini-player about the current song. Should be called whenever the
     * currently playing song changes.
     *
     * @param songTitle  Song title.
     * @param songAlbum  Song album.
     * @param songArtist Song artist.
     */
    public void setPlayingSongInfo(String songTitle, String songAlbum, String songArtist) {
        miniPlayerSongInfo.setText(songTitle + "     |     " + songAlbum + "     |     " + songArtist);
        miniPlayerSongInfo.repaint();
    }

    /**
     * Sets the icon of the play/pause button.
     *
     * @param state BUTTON_ICON_PLAY (0) to display the play icon (paused)
     *              and BUTTON_ICON_PAUSE (1) to display the pause icon (playing).
     */
    public void setPlayPauseButtonIcon(int state) {
        switch (state) {
            case 0 -> miniPlayerPlayPauseButton.setIcon(Icons24.play);
            case 1 -> miniPlayerPlayPauseButton.setIcon(Icons24.pause);
        }
    }

    /**
     * Enables or disables the Shuffle button.
     *
     * @param enable True to enable and false to disable.
     */
    public void setEnabledShuffleButton(Boolean enable) {
        miniPlayerShuffleButton.setEnabled(enable);
    }

    /**
     * Enables or disables the Previous button.
     *
     * @param enable True to enable and false to disable.
     */
    public void setEnabledPreviousButton(Boolean enable) {
        miniPlayerPreviousButton.setEnabled(enable);
    }

    /**
     * Enables or disables the Play/Pause button.
     *
     * @param enable True to enable and false to disable.
     */
    public void setEnabledPlayPauseButton(Boolean enable) {
        miniPlayerPlayPauseButton.setEnabled(enable);
    }

    /**
     * Enables or disables the Stop button.
     *
     * @param enable True to enable and false to disable.
     */
    public void setEnabledStopButton(Boolean enable) {
        miniPlayerStopButton.setEnabled(enable);
    }

    /**
     * Enables or disables the Next button.
     *
     * @param enable True to enable and false to disable.
     */
    public void setEnabledNextButton(Boolean enable) {
        miniPlayerNextButton.setEnabled(enable);
    }

    /**
     * Enables or disables the Loop button.
     *
     * @param enable True to enable and false to disable.
     */
    public void setEnabledLoopButton(Boolean enable) {
        miniPlayerLoopButton.setEnabled(enable);
    }

    /**
     * Enables or disables the Scrubber.
     *
     * @param enable True to enable and false to disable.
     */
    public void setEnabledScrubber(Boolean enable) {
        miniPlayerScrubber.setEnabled(enable);
    }

    /**
     * Updates the labels and scrubber values in the mini-player.
     *
     * @param currentTime Current time of the current song in milliseconds.
     * @param totalTime   Total time of the current song in milliseconds.
     */
    public void setTime(int currentTime, int totalTime) {
        miniPlayerCurrentTime.setText(SecondsToString.currentTimeToString(currentTime / 1000, totalTime / 1000));
        miniPlayerTotalTime.setText(SecondsToString.lengthToString(totalTime / 1000));
        miniPlayerScrubber.setMaximum(totalTime);
        miniPlayerScrubber.setValue(currentTime);
    }

    /**
     * Resets mini-player to default values and disables buttons. Should be called whenever the 'stop' button is pressed.
     */
    public void resetMiniPlayer() {
        miniPlayerCurrentTime.setText("- - : - -");
        miniPlayerTotalTime.setText("- - : - -");
        miniPlayerSongInfo.setText("");
        miniPlayerScrubber.setMaximum(0);
        setPlayPauseButtonIcon(BUTTON_ICON_PLAY);
        setEnabledPreviousButton(false);
        setEnabledNextButton(false);
        setEnabledPlayPauseButton(false);
        setEnabledStopButton(false);
        setEnabledScrubber(false);
    }

    /**
     * @return the ID of the selected song in the queue. Should be called whenever the 'Play Now' and 'Remove'
     * buttons are pressed.
     */
    public String getSelectedSong() {
        return String.valueOf((queueList.getValueAt(queueList.getSelectedRow(), 5)));
    }

    /**
     * Should be called whenever the user manually changes the scrubber state with the cursor.
     *
     * @return the current value of the scrubber.
     */
    public int getScrubberValue() {
        return miniPlayerScrubber.getValue();
    }

    /**
     * Opens a file chooser and returns a Song object with information parsed from the file.
     * If information can't be parsed from the file a dialog is open to input song info.
     *
     * @return chosen MP3 or Null if cancelled.
     */
    public Song openFileChooser() throws IOException, BitstreamException, UnsupportedTagException, InvalidDataException {
        CustomFileChooser fileChooser = new CustomFileChooser();
        fileChooser.setCurrentDirectory(new File
                (System.getProperty("user.home") + System.getProperty("file.separator") + "Downloads"));
        int fileChooserReturnValue = fileChooser.showOpenDialog(this.window);

        if (fileChooserReturnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String title = "";
            String album = "";
            String artist = "";
            String year = "";
            float msLength;
            String strLength;
            String filePath = file.getPath();
            int fileSize = (int) Files.size(Path.of(filePath));
            int numFrames;
            float msPerFrame;
            int frameSize;

            // Try to get ID3 info
            Mp3File mp3File = new Mp3File(file);
            if (mp3File.hasId3v1Tag()) {
                ID3v1 id3 = mp3File.getId3v1Tag();
                title = id3.getTitle();
                album = id3.getAlbum();
                artist = id3.getArtist();
                year = id3.getYear();
            } else if (mp3File.hasId3v2Tag()) {
                ID3v2 id3 = mp3File.getId3v2Tag();
                title = id3.getTitle();
                album = id3.getAlbum();
                artist = id3.getArtist();
                year = id3.getYear();
            }
            if (title == null || title.isBlank() || album == null || album.isBlank() || artist == null || artist.isBlank() || year == null || year.isBlank()) {
                JPanel songInfoPanel = new JPanel();

                JPanel messagePanel = new JPanel();
                JPanel songTitlePanel = new JPanel();
                JPanel songAlbumPanel = new JPanel();
                JPanel songArtistPanel = new JPanel();
                JPanel songYearPanel = new JPanel();

                String message = "<html><body>Some ID3 tags could not be parsed from this file.<br>Would you like to fill in the fields bellow?</body></html>";
                JLabel messageLabel = new JLabel(message);
                JLabel songTitleLabel = new JLabel("Title:");
                JLabel songAlbumLabel = new JLabel("Album:");
                JLabel songArtistLabel = new JLabel("Artist:");
                JLabel songYearLabel = new JLabel("Year:");

                JTextField songTitleField = new JTextField();
                JTextField songAlbumField = new JTextField();
                JTextField songArtistField = new JTextField();
                JTextField songYearField = new JTextField();
                PlainDocument yearDocument = (PlainDocument) songYearField.getDocument();
                yearDocument.setDocumentFilter(new intDocumentFilter());

                songInfoPanel.setLayout(new GridLayout(5, 1));
                songInfoPanel.add(messagePanel);
                songInfoPanel.add(songTitlePanel);
                songInfoPanel.add(songAlbumPanel);
                songInfoPanel.add(songArtistPanel);
                songInfoPanel.add(songYearPanel);

                messagePanel.setLayout(new GridLayout(1, 1));
                messagePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                messagePanel.add(messageLabel);

                songTitlePanel.setLayout(new GridLayout(2, 1));
                songTitlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                songTitlePanel.add(songTitleLabel);
                songTitlePanel.add(songTitleField);

                songAlbumPanel.setLayout(new GridLayout(2, 1));
                songAlbumPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                songAlbumPanel.add(songAlbumLabel);
                songAlbumPanel.add(songAlbumField);

                songArtistPanel.setLayout(new GridLayout(2, 1));
                songArtistPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                songArtistPanel.add(songArtistLabel);
                songArtistPanel.add(songArtistField);

                songYearPanel.setLayout(new GridLayout(2, 1));
                songYearPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                songYearPanel.add(songYearLabel);
                songYearPanel.add(songYearField);

                if (title != null && !title.isBlank()) {
                    songTitleField.setText(title);
                    songTitleField.setEditable(false);
                }
                if (album != null && !album.isBlank()) {
                    songAlbumField.setText(album);
                    songAlbumField.setEditable(false);
                }
                if (artist != null && !artist.isBlank()) {
                    songArtistField.setText(artist);
                    songArtistField.setEditable(false);
                }
                if (year != null && !year.isBlank()) {
                    songYearField.setText(year);
                    songYearField.setEditable(false);
                }

                int result = JOptionPane.showConfirmDialog(this.window, songInfoPanel,
                        "Add song info", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    title = songTitleField.getText();
                    album = songAlbumField.getText();
                    artist = songArtistField.getText();
                    year = songYearField.getText();
                }
            }

            numFrames = mp3File.getFrameCount();
            msLength = mp3File.getLengthInMilliseconds();
            msPerFrame = msLength / numFrames;

            Duration duration = Duration.ofMillis((long) msLength);
            long HH = duration.toHours();
            long MM = duration.toMinutesPart();
            long SS = duration.toSecondsPart();
            strLength = String.format("%d:%02d:%02d", HH, MM, SS);

            if (title == null || title.isBlank()) title = "Untitled";
            if (album == null || album.isBlank()) album = "Untitled";
            if (artist == null || artist.isBlank()) artist = "Unknown";
            if (year == null || year.isBlank()) year = "Unknown";

            String uuid = UUID.randomUUID().toString();
            return new Song(uuid, title, album, artist, year, strLength, msLength, filePath, fileSize, numFrames, msPerFrame);
        } else {
            return null;
        }
    }
}

/**
 * DocumentFilter that only accepts int values with at most 4 digits.
 */
final class intDocumentFilter extends DocumentFilter {
    private boolean isValid(String testText) {
        if (testText.length() > 4) {
            return false;
        }
        if (testText.isEmpty()) {
            return true;
        }
        int intValue;
        try {
            intValue = Integer.parseInt(testText.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return intValue >= 0 && intValue <= 9999;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text,
                             AttributeSet attr) throws BadLocationException {
        StringBuilder sb = new StringBuilder();
        sb.append(fb.getDocument().getText(0, fb.getDocument().getLength()));
        sb.insert(offset, text);
        if (isValid(sb.toString())) {
            super.insertString(fb, offset, text, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length,
                        String text, AttributeSet attrs) throws BadLocationException {
        StringBuilder sb = new StringBuilder();
        sb.append(fb.getDocument().getText(0, fb.getDocument().getLength()));
        int end = offset + length;
        sb.replace(offset, end, text);
        if (isValid(sb.toString())) {
            super.replace(fb, offset, length, text, attrs);
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
        StringBuilder sb = new StringBuilder();
        sb.append(fb.getDocument().getText(0, fb.getDocument().getLength()));
        int end = offset + length;
        sb.delete(offset, end);
        if (isValid(sb.toString())) {
            super.remove(fb, offset, length);
        }
    }
}
