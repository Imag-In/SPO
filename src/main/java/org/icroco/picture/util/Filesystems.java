package org.icroco.picture.util;


import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;


public class Filesystems {
    static final double MB = 1024 * 1024;


    public static void main(String[] args)
            throws IOException {
        FileSystem fs = FileSystems.getDefault(); // close?

        // list of roots does not contain named mounts on windows:
        Iterable<Path> roots = fs.getRootDirectories();
        for (Path f : roots) {
            System.out.printf("root dir %s%n", f.toString());
        }
        System.out.println();

        // list of filestores does not either
        Iterable<FileStore> rootStores = fs.getFileStores();
        printStores(rootStores);

        System.out.println("--");

//        // directly addressed the volume shows
//        Path disk = Paths.get("C:", "temp", "disk").toAbsolutePath();
//        FileStore fss = Files.getFileStore(disk);
//        printStore(fss);

        //Path vol = Paths.get("\\\\?\\Volume{ff0418c4-f533-11df-80e2-806e6f6e6963}\\").toAbsolutePath();
    }


    private static void printStores(Iterable<FileStore> stores) {
        stores.iterator().forEachRemaining(Filesystems::printStore);
    }


    private static void printStore(FileStore f) {
        try {
            // extract attributes
            String vsn         = getAtt(f, "volume:vsn");
            String isRemovable = getAtt(f, "volume:isRemovable");
            String isCdrom     = getAtt(f, "volume:isCdrom");

            // extract mount point
            String root = f.toString();
            root = "/" + root.substring(root.lastIndexOf('(') + 1, root.lastIndexOf(')')) + "\\";
            Path rootPath;
            rootPath = new File(root).getCanonicalFile().toPath();

            // more attributes
            //String owner = Files.getOwner(rootPath).getName();

            System.out.printf("%-40s %-10s %,10.2f MB %,10.2f MB %s r?=%s cd?=%s vsn=%s on %s%n",
                              f.toString(),
                              "(" + f.type() + ")",
                              f.getUsableSpace() / MB,
                              f.getTotalSpace() / MB,
                              (f.isReadOnly() ? "ro" : "rw"),
                              isRemovable,
                              isCdrom,
                              vsn,
                              rootPath
                              //,owner
            );
        } catch (IOException e) {
            // streams buster
            throw new RuntimeException(e);
        }
    }


    private static String getAtt(FileStore f, String name) {
        try {
            Object result = f.getAttribute(name);
            if (result == null) {
                return "-";
            }
            if (result instanceof Boolean) {
                return ((Boolean) result).toString();
            }
            if (result instanceof String) {
                return ((String) result).trim();
            }
            if (result instanceof Integer) {
                String hex = Integer.toUnsignedString(((Integer) result).intValue(), 16);
                return hex.substring(0, 4) + "-" + hex.substring(4, 8) + " " + Integer.toUnsignedString(((Integer) result).intValue());
            }
            return String.valueOf(result);
        } catch (Exception ignored) { /* do nothing */ }
        return "N/A";
    }

}