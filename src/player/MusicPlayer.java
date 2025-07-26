package player;

import ui.MusicPlayerUI;

import javax.sound.sampled.*;
import java.io.File;
import java.util.List;
import javax.swing.Timer;
import java.util.Random; // Import Random class

public class MusicPlayer {
    Clip clip;
    private long clipPosition = 0; // Stores position in microseconds
    private boolean isPaused = false;
    private boolean shuffle = false;
    private boolean repeat = false; // Now specifically "Repeat Song"
    private boolean repeatFolder = false; // Flag for "Repeat Folder"
    private boolean playNextFolder = false; // Flag for "Normal" mode (play next folder)

    private List<File> currentSongs;
    private int currentSongIndex = 0;
    private Timer progressTimer;
    private MusicPlayerUI ui;

    public MusicPlayer(MusicPlayerUI ui) {
        this.ui = ui;
    }

    public void setPlaylist(List<File> songs) {
        currentSongs = songs;
        currentSongIndex = 0; // Reset index when a new playlist is set
        stop(); // Ensure any previous playback is stopped
    }

    public void play(int index) {
        if (currentSongs == null || currentSongs.isEmpty() || index < 0 || index >= currentSongs.size()) {
            System.out.println("DEBUG: play() called but conditions not met. currentSongs empty? " + (currentSongs == null || currentSongs.isEmpty()) + ", index: " + index + ", list size: " + (currentSongs != null ? currentSongs.size() : "null"));
            System.out.println("Invalid song index or no playlist loaded.");
            ui.setPlayPauseButtonState(false); // Reset button if unable to play
            stop(); // Ensure player is stopped if nothing can be played
            return;
        }

        // Declare 'song' here to make it effectively final for the LineListener
        final File song = currentSongs.get(index);

        try {
            stop(); // Stop any currently playing song before starting a new one

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(song);
            clip = AudioSystem.getClip();
            clip.open(audioStream);

            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    // Check if the clip naturally reached its end and is not paused
                    if (clip != null && clip.getMicrosecondPosition() >= clip.getMicrosecondLength() && !isPaused) {
                        if (repeat) { // Priority 1: Repeat Song mode
                            play(currentSongIndex);
                        } else if (shuffle) { // Priority 2: Shuffle mode
                            playNext();
                        } else if (repeatFolder) { // Priority 3: Repeat Folder mode
                            // If it's the last song in the folder, go back to the first song.
                            // Otherwise, just play the next song in the folder.
                            if (currentSongIndex < currentSongs.size() - 1) {
                                playNext(); // Go to next song in folder normally
                            } else {
                                play(0); // Repeat the folder from the first song
                            }
                        } else if (currentSongIndex < currentSongs.size() - 1) { // Priority 4: Normal progression within folder
                            playNext();
                        } else if (playNextFolder) { // Priority 5: Normal mode (play next folder) - only when the last song of the current folder finishes
                            ui.playNextFolder(); // Delegate to UI to change playlist
                        } else { // No specific mode (e.g., reached end of last song in the last folder, or no mode active)
                            stop();
                            ui.setPlayPauseButtonState(false);
                        }
                    }
                }
            });

            clip.start();
            currentSongIndex = index;
            isPaused = false; // Reset paused state as we're starting a new song
            clipPosition = 0; // Reset clip position for a new song
            startProgressTimer();
            ui.updateSongSelection(index);
            ui.setPlayPauseButtonState(true); // Set UI button to "Pause"
        } catch (Exception e) {
            e.printStackTrace();
            // Use 'song.getName()' here, as 'song' is now accessible
            System.err.println("Error playing song: " + song.getName() + " - " + e.getMessage());
            ui.setPlayPauseButtonState(false); // Reset button if error occurs
            stop(); // Ensure player is stopped on error
        }
    }

    public void pause() {
        if (clip != null && clip.isRunning()) {
            clipPosition = clip.getMicrosecondPosition();
            clip.stop();
            isPaused = true;
            stopProgressTimer();
            ui.setPlayPauseButtonState(false); // Set UI button to "Play"
        }
    }

    public void resume() {
        if (clip != null && !clip.isRunning() && isPaused) {
            clip.setMicrosecondPosition(clipPosition);
            clip.start();
            isPaused = false;
            startProgressTimer();
            ui.setPlayPauseButtonState(true); // Set UI button to "Pause"
        }
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        isPaused = false;
        clipPosition = 0;
        stopProgressTimer();
        ui.resetProgressSlider();
        ui.setPlayPauseButtonState(false); // Set UI button to "Play"
    }

    /**
     * Seeks to a specific position in the current song.
     * @param positionMicros The position to seek to, in microseconds.
     */
    public void seek(long positionMicros) {
        if (clip != null && clip.isOpen()) {
            if (positionMicros >= 0 && positionMicros <= clip.getMicrosecondLength()) {
                clip.setMicrosecondPosition(positionMicros);
                clipPosition = positionMicros; // Update stored position
                // REMOVED: updateProgress(); // This line was removed to prevent conflicts
                if (!clip.isRunning() && !isPaused) { // If stopped and not paused, start playing (e.g., after seeking in a stopped state)
                    clip.start();
                    ui.setPlayPauseButtonState(true);
                }
            } else {
                System.out.println("Seek position out of bounds. Requested: " + positionMicros + ", Max: " + clip.getMicrosecondLength());
            }
        }
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        // Ensure other playback modes are off when shuffle is active
        if (shuffle) {
            this.repeat = false;
            this.repeatFolder = false;
            this.playNextFolder = false;
        }
    }

    public void setRepeat(boolean repeat) { // Repeat Song
        this.repeat = repeat;
        // Ensure other playback modes are off when repeat song is active
        if (repeat) {
            this.shuffle = false;
            this.repeatFolder = false;
            this.playNextFolder = false;
        }
    }

    public void setRepeatFolder(boolean repeatFolder) { // New method for Repeat Folder
        this.repeatFolder = repeatFolder;
        // Ensure other playback modes are off when repeat folder is active
        if (repeatFolder) {
            this.shuffle = false;
            this.repeat = false;
            this.playNextFolder = false;
        }
    }

    public void setPlayNextFolder(boolean playNextFolder) { // New method for Normal mode
        this.playNextFolder = playNextFolder;
        // Ensure other playback modes are off when normal mode is active
        if (playNextFolder) {
            this.shuffle = false;
            this.repeat = false;
            this.repeatFolder = false;
        }
    }

    public void playNext() {
        if (currentSongs != null && !currentSongs.isEmpty()) {
            if (shuffle) {
                Random random = new Random(); // Initialize Random object
                int newIndex;
                do {
                    newIndex = random.nextInt(currentSongs.size());
                } while (currentSongs.size() > 1 && newIndex == currentSongIndex); // Avoid playing same song immediately if more than one song
                currentSongIndex = newIndex;
            } else {
                currentSongIndex = (currentSongIndex + 1) % currentSongs.size();
            }
            play(currentSongIndex);
        } else {
            stop(); // No songs to play, stop and reset UI
            ui.setPlayPauseButtonState(false);
        }
    }

    public void playPrevious() {
        if (currentSongs != null && !currentSongs.isEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + currentSongs.size()) % currentSongs.size();
            play(currentSongIndex);
        } else {
            stop(); // No songs to play, stop and reset UI
            ui.setPlayPauseButtonState(false);
        }
    }

    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean hasActiveClip() {
        return clip != null;
    }

    public long getTotalDurationMicros() {
        return clip != null ? clip.getMicrosecondLength() : 0;
    }

    private void updateProgress() {
        if (clip != null && clip.isOpen()) {
            long currentPositionMicros = clip.getMicrosecondPosition();
            long totalDurationMicros = clip.getMicrosecondLength();

            if (totalDurationMicros > 0) {
                int progress = (int) ((double) currentPositionMicros / totalDurationMicros * 100);
                ui.updateProgressSlider(progress, currentPositionMicros / 1000, totalDurationMicros / 1000);
            }
        }
    }

    private void startProgressTimer() {
        stopProgressTimer(); // Ensure no multiple timers are running
        progressTimer = new Timer(100, e -> updateProgress()); // Update every 100ms
        progressTimer.start();
    }

    private void stopProgressTimer() {
        if (progressTimer != null && progressTimer.isRunning()) {
            progressTimer.stop();
            progressTimer = null;
        }
    }

    // New methods to allow UI to control the progress timer
    public void stopProgressTimerExternal() {
        stopProgressTimer();
    }

    public void startProgressTimerExternal() {
        startProgressTimer();
    }
}