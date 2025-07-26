package ui;

import player.MusicPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.Timer; // Explicitly importing the correct Timer class

public class MusicPlayerUI extends JFrame {
    private JComboBox<String> playlistBox = new JComboBox<>();
    private DefaultListModel<String> songListModel = new DefaultListModel<>();
    private JList<String> songList = new JList<>(songListModel);
    private Map<String, File> songMap = new HashMap<>();
    private MusicPlayer player = new MusicPlayer(this);
    private java.util.List<File> currentSongs = new ArrayList<>();
    private java.util.List<String> allPlaylists = new ArrayList<>(); // To manage folder progression

    private JSlider progressSlider = new JSlider(0, 100, 0);
    private JLabel progressLabel = new JLabel("0:00 / 0:00");

    private JButton playPauseBtn;
    private JButton nextBtn;
    private JButton prevBtn;
    private JButton playbackModeBtn; // Button for cycling modes

    private boolean isPlaying = false; // To manage play/pause button state
    // New modes: 0: Normal, 1: Shuffle, 2: Repeat Song, 3: Repeat Folder
    private int currentMode = 0; // Initial state: Normal

    // Default constructor (if no specific playlist is provided)
    public MusicPlayerUI() {
        this(null); // Call the overloaded constructor with a null initial playlist
    }

    // New constructor to load a specific playlist on startup
    public MusicPlayerUI(String initialPlaylistName) {
        setTitle("Java Swing Music Player");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 450);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Select Playlist:"));
        topPanel.add(playlistBox);

        playPauseBtn = new JButton("▶ Play");
        nextBtn = new JButton("⏭ Next");
        prevBtn = new JButton("⏮ Prev");
        playbackModeBtn = new JButton("Mode: Normal"); // Initial state for mode button

        // Progress Slider
        progressSlider.setMinimum(0);
        progressSlider.setMaximum(100);
        progressSlider.setValue(0);
        progressSlider.setEnabled(false); // Initially disabled

        JPanel controls = new JPanel();
        controls.add(playPauseBtn);
        controls.add(prevBtn);
        controls.add(nextBtn);
        controls.add(playbackModeBtn); // Add the new mode button

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(new JLabel("Progress:"), BorderLayout.WEST);
        progressPanel.add(progressSlider, BorderLayout.CENTER);
        progressPanel.add(progressLabel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controls, BorderLayout.NORTH);
        bottomPanel.add(progressPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(songList), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Load playlists
        File base = new File("src/playlists");
        if (base.exists()) {
            for (File folder : base.listFiles(File::isDirectory)) {
                String folderName = folder.getName();
                playlistBox.addItem(folderName);
                allPlaylists.add(folderName); // Store all playlist names
            }
        }

        playlistBox.addActionListener(e -> {
            System.out.println("Playlist selected: " + playlistBox.getSelectedItem());
            loadPlaylist((String) playlistBox.getSelectedItem());
        });

        // Changed from double-click to single-click
        songList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Mouse click detected on song list. Click count: " + e.getClickCount());
                if (e.getClickCount() == 1) { // Changed to single click
                    int index = songList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        System.out.println("Single-clicked on song index: " + index);
                        player.play(index);
                        // setPlayPauseButtonState(true); // Handled by player.play()
                    } else {
                        System.out.println("Clicked, but no valid song index found.");
                    }
                }
            }
        });

        playPauseBtn.addActionListener(e -> {
            if (isPlaying) {
                System.out.println("Pause button clicked.");
                player.pause(); // player.pause() will call setPlayPauseButtonState(false)
            } else {
                if (player.isPaused()) {
                    System.out.println("Resume button clicked.");
                    player.resume(); // player.resume() will call setPlayPauseButtonState(true)
                } else {
                    int index = songList.getSelectedIndex();
                    if (index >= 0) {
                        System.out.println("Play button clicked. Playing song index: " + index);
                        player.play(index); // player.play() will call setPlayPauseButtonState(true)
                    } else if (!currentSongs.isEmpty()) {
                        System.out.println("No song selected, playing first song.");
                        player.play(0); // player.play() will call setPlayPauseButtonState(true)
                        songList.setSelectedIndex(0);
                    } else {
                        // If no songs are available, ensure the button is in 'Play' state.
                        setPlayPauseButtonState(false);
                    }
                }
                // Removed the redundant if (player.isPlaying() || player.isPaused()) block
            }
        });

        nextBtn.addActionListener(e -> {
            System.out.println("Next button clicked.");
            player.playNext();
            // setPlayPauseButtonState(true); // Handled by player.playNext() internally calling player.play()
        });

        prevBtn.addActionListener(e -> {
            System.out.println("Previous button clicked.");
            player.playPrevious();
            // setPlayPauseButtonState(true); // Handled by player.playPrevious() internally calling player.play()
        });

        // Action Listener for the new playback mode button
        playbackModeBtn.addActionListener(e -> {
            currentMode = (currentMode + 1) % 4; // Cycles 0 -> 1 -> 2 -> 3 -> 0 (Normal -> Shuffle -> Repeat Song -> Repeat Folder -> Normal)
            updatePlaybackMode();
        });

        // Progress slider listener
        progressSlider.addMouseListener(new MouseAdapter() {
            private boolean wasPlayingBeforeDrag = false;

            @Override
            public void mousePressed(MouseEvent e) {
                // Pause if playing and record state
                if (player.isPlaying()) {
                    wasPlayingBeforeDrag = true;
                    player.pause();
                } else {
                    wasPlayingBeforeDrag = false;
                }
                // Stop the progress timer when the user starts interacting with the slider
                player.stopProgressTimerExternal(); // New method call
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (progressSlider.isEnabled() && player.hasActiveClip()) {
                    // On release, seek to the final position of the slider
                    long duration = player.getTotalDurationMicros();
                    long seekPositionMicros = (long) ((double) progressSlider.getValue() / 100.0 * duration);
                    System.out.println("Progress slider released. Seeking to: " + seekPositionMicros / 1000 + " ms");
                    player.seek(seekPositionMicros);
                    // NEW: Directly update UI with the intended seek position
                    updateProgressSlider(progressSlider.getValue(), seekPositionMicros / 1000, duration / 1000);
                }

                // Resume if it was playing before drag/click
                if (wasPlayingBeforeDrag) {
                    player.resume();
                }
                // Restart the progress timer after the user releases the slider
                player.startProgressTimerExternal(); // New method call
            }
        });

        // Add a ChangeListener for real-time scrubbing (while dragging)
        progressSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (progressSlider.getValueIsAdjusting()) { // True when slider is being dragged
                    if (player.hasActiveClip()) {
                        long duration = player.getTotalDurationMicros();
                        long seekPositionMicros = (long) ((double) progressSlider.getValue() / 100.0 * duration);
                        player.seek(seekPositionMicros);
                        // NEW: Directly update UI with the intended seek position during drag
                        updateProgressSlider(progressSlider.getValue(), seekPositionMicros / 1000, duration / 1000);
                    }
                }
            }
        });


        // Load the initial playlist if provided
        if (initialPlaylistName != null && allPlaylists.contains(initialPlaylistName)) {
            playlistBox.setSelectedItem(initialPlaylistName); // This will trigger the loadPlaylist action
        } else if (!allPlaylists.isEmpty()) {
            // If no specific playlist or playlist not found, select the first one by default
            playlistBox.setSelectedIndex(0); // This will trigger the loadPlaylist action
        }

        setVisible(true);
    }

    private void loadPlaylist(String name) {
        songListModel.clear();
        songMap.clear();
        currentSongs.clear();
        player.stop();
        resetProgressSlider();
        setPlayPauseButtonState(false);
        // Reset playback mode to Normal when a new playlist is loaded
        currentMode = 0;
        updatePlaybackMode();


        File folder = new File("src/playlists", name);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().toLowerCase().endsWith(".wav")) {
                        System.out.println("Loading song: " + f.getName());
                        songListModel.addElement(f.getName());
                        songMap.put(f.getName(), f);
                        currentSongs.add(f);
                    }
                }
            }

            if (!currentSongs.isEmpty()) {
                System.out.println("Playlist loaded with " + currentSongs.size() + " songs.");
                player.setPlaylist(currentSongs);
            }
        }
    }

    public void updateProgressSlider(int progress, long currentPositionMillis, long totalDurationMillis) {
        progressSlider.setValue(progress);

        long currentMinutes = (currentPositionMillis / 1000 / 60);
        long currentSeconds = (currentPositionMillis / 1000) % 60;
        String currentTime = String.format("%d:%02d", currentMinutes, currentSeconds);

        long totalMinutes = (totalDurationMillis / 1000 / 60);
        long totalSeconds = (totalDurationMillis / 1000) % 60;
        String totalTime = String.format("%d:%02d", totalMinutes, totalSeconds);

        progressLabel.setText(currentTime + " / " + totalTime);
        progressSlider.setEnabled(true);
    }

    public void resetProgressSlider() {
        progressSlider.setValue(0);
        progressSlider.setEnabled(false);
        progressLabel.setText("0:00 / 0:00");
    }

    public void updateSongSelection(int index) {
        songList.setSelectedIndex(index);
        songList.ensureIndexIsVisible(index);
    }

    public void setPlayPauseButtonState(boolean playing) {
        isPlaying = playing;
        if (playing) {
            playPauseBtn.setText("⏸ Pause");
        } else {
            playPauseBtn.setText("▶ Play");
        }
    }

    /**
     * Cycles through and updates the current playback mode.
     * 0: Normal (play next song in folder, then next folder)
     * 1: Shuffle (random song in current folder)
     * 2: Repeat Song (repeat current song)
     * 3: Repeat Folder (repeat current folder)
     */
    private void updatePlaybackMode() {
        // Reset all flags in player first to ensure only the selected mode is active
        player.setShuffle(false);
        player.setRepeat(false); // This will now mean "repeat song"
        player.setRepeatFolder(false); // New flag for repeat folder
        player.setPlayNextFolder(false); // New flag for normal mode

        switch (currentMode) {
            case 0: // Normal
                playbackModeBtn.setText("Mode: Normal");
                player.setPlayNextFolder(true); // Enable playing next folder
                break;
            case 1: // Shuffle
                playbackModeBtn.setText("Mode: 🔀 Shuffle");
                player.setShuffle(true);
                break;
            case 2: // Repeat Song
                playbackModeBtn.setText("Mode: 🔁 Repeat Song");
                player.setRepeat(true);
                break;
            case 3: // Repeat Folder
                playbackModeBtn.setText("Mode: 🔂 Repeat PlayList");
                player.setRepeatFolder(true);
                break;
        }
        System.out.println("Playback mode set to: " + playbackModeBtn.getText());
    }

    /**
     * Called by MusicPlayer when a folder finishes in "Normal" mode.
     * Advances to the next playlist in the JComboBox.
     */
    public void playNextFolder() {
        int currentIndex = playlistBox.getSelectedIndex();
        if (currentIndex != -1) {
            int nextIndex = (currentIndex + 1) % allPlaylists.size();
            playlistBox.setSelectedIndex(nextIndex); // This will trigger loadPlaylist
            // Automatically play the first song of the new playlist
            if (!currentSongs.isEmpty()) {
                player.play(0);
                songList.setSelectedIndex(0);
                // setPlayPauseButtonState(true); // Handled by player.play(0)
            } else {
                setPlayPauseButtonState(false);
            }
        }
    }
}