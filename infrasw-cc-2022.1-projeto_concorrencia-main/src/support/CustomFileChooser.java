package support;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

public final class CustomFileChooser extends JFileChooser {
    public CustomFileChooser() {
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MP3", "mp3");
        this.setFileSelectionMode(JFileChooser.FILES_ONLY);
        this.setAcceptAllFileFilterUsed(false);
        this.setFileFilter(filter);
        this.setPreferredSize(new Dimension(700, 550));
    }
}
