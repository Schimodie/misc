package org.schimodie.albums_to_listen_to.database;

import org.schimodie.albums_to_listen_to.bean.Album;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Storage {
    private static final String ALBUMS_SERDE_DIR = "/home/schimodie/Code/Java/misc/db/metalstorm-lastfm-spotify-playlist";

    private Storage() {
    }

    public static boolean containsFile(String fileName) {
        return createFile(fileName).exists();
    }

    public static List<Album> readAlbums(String fileName) {
        File albumsFile = createFile(fileName);
        if (!albumsFile.exists()) {
            throw new RuntimeException(String.format("File '%s' does not exist", albumsFile.getAbsolutePath()));
        }

        List<Album> albums = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(albumsFile))) {
            boolean continueReading = true;
            while (continueReading) {
                String line = reader.readLine();
                if (line != null) {
                    albums.add(Album.from(line));
                } else {
                    continueReading = false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return albums;
    }

    public static void writeAlbums(List<Album> albums, String fileName) {
        File albumsFile = createFile(fileName);

        if (!albumsFile.getParentFile().exists()) {
            if (!albumsFile.getParentFile().mkdirs()) {
                throw new RuntimeException(
                        String.format("Could not create directory '%s'", albumsFile.getParentFile().getAbsolutePath()));
            }
        }

        if (!albumsFile.exists()) {
            try {
                if (!albumsFile.createNewFile()) {
                    throw new RuntimeException(
                            String.format("Could not create file '%s'", albumsFile.getAbsolutePath()));
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        String.format("Could not create file '%s'", albumsFile.getAbsolutePath()), e);
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(albumsFile))) {
            for (Album album : albums) {
                writer.write(album.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File createFile(String fileName) {
        return new File(String.format("%s/%s", ALBUMS_SERDE_DIR, fileName));
    }
}
