package externalsort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Sorts a file externally on disk by using merge-sort
 * 
 * Based on the external memory sorting java implementation originally written by:
 * (in alphabetical order) Philippe Beaudoin, Eleftherios Chetzakis, Jon
 * Elsas, Christan Grant, Daniel Haran, Daniel Lemire, Sugumaran Harikrishnan,
 * Amit Jain, Thomas Mueller, Andreas Spitz, Jerry Yang.
 * First published: April 2010 originally posted at
 * http://lemire.me/blog/archives/2010/04/01/external-memory-sorting-in-java/
 */
public class ParallelDiskMergeSort {
    
    private long totalLinesInFile;
    private int maxLinesPerTmpFile;
    private int writeBufferSize;
    private Comparator<String> stringComparator;
    
    Comparator<FileBuffer> fileComparator = new Comparator<FileBuffer>() {
        @Override
        public int compare(FileBuffer i, FileBuffer j) {
            return stringComparator.compare(i.peek(), j.peek());
        }
    };
    
    Comparator<LinkedList<String>> listComparator = new Comparator<LinkedList<String>>() {
        @Override
        public int compare(LinkedList<String> i, LinkedList<String> j) {
            return stringComparator.compare(i.peek(), j.peek());
        }
    };
    
    public ParallelDiskMergeSort(long totalLines, int maxLinesPerTmpFile, int writeBufferSize,
                                 Comparator<String> stringComparator) {
        this.totalLinesInFile = totalLines;
        this.maxLinesPerTmpFile = maxLinesPerTmpFile;
        this.writeBufferSize = writeBufferSize;
        this.stringComparator = stringComparator;
    }
    
    /**
     * This merges a bunch of temporary flat files
     * 
     * @param files       The {@link List} of sorted {@link File}s to be merged.
     * @param outputfile  The output {@link File} to merge the results to.
     * @throws            IOException
     */
    private void mergeSortedFiles(List<File> files, File outputfile) throws IOException {
        
        // create priority queue for keeping files sorted by their top most string
        PriorityQueue<FileBuffer> pq = new PriorityQueue<FileBuffer>(11, fileComparator);
        
        // add temporary input file buffers to priority queue
        for (File f : files) {
            InputStream in = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            FileBuffer bfb = new FileBuffer(br);
            pq.add(bfb);
        }
        
        // create output file buffer
        BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfile),"UTF-8"), writeBufferSize);
        
        // while there are still files that contain strings (lines), pop the lines and add them to output file
        try {
            long nextpercent = totalLinesInFile / 100;
            int percentcount = 0;
            long currentLine = 0;
            
              while (pq.size() > 0) {
                   FileBuffer bfb = pq.poll();
                String r = bfb.pop();
                fbw.write(r);
                fbw.newLine();
                
                if (++currentLine == nextpercent) {
                    nextpercent += totalLinesInFile / 100;
                    percentcount++;
                    System.out.print("\rMerged "  + percentcount +" % of lines." );
                }
                
                if (bfb.empty()) {
                     bfb.fbr.close();
                } else {
                      pq.add(bfb);
                }
            }
        } finally {
               fbw.close();
            for (FileBuffer bfb : pq) {
                  bfb.close();
            }
            //System.out.println();
        }
        
        // clean up temporary files
        for (File f : files) {
            f.delete();
        }
    }

    /**
     * Sort a list and save it to a temporary file
     * 
     * @return the file containing the sorted data
     * @param tmplist      data to be sorted
     * @param tmpdirectory location of the temporary files (set to null for
     *                     default location)
     * @throws IOException
     */
    private File sortAndSave(String[] fileContent, File tmpdirectory) throws IOException {
        
        Arrays.parallelSort(fileContent, stringComparator);
        
        // create a new temporary file and set it to delete itself after use
        File newtmpfile = File.createTempFile("sortInBatch"," flatfile",  tmpdirectory);
        newtmpfile.deleteOnExit();
        
        // create a buffered writer for the file
        OutputStream out = new FileOutputStream(newtmpfile);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out,"UTF-8"), writeBufferSize);
        
        for (String s : fileContent) {
            bw.write(s);
            bw.newLine();
        }
        bw.close();
        
        return newtmpfile;
    }

    /**
     * @param fbr            data source
     * @param tmpdirectory    location of the temporary files (set to null for
     *                        default location)
     * @return a list of temporary flat files
     * @throws IOException
     */
    private List<File> sortInBatch(final BufferedReader fbr, final File tmpdirectory) throws IOException {
        
        int nFiles = (int) Math.ceil((double) totalLinesInFile / (double) maxLinesPerTmpFile);
        long remainingLines = totalLinesInFile;
        int arrayLength = maxLinesPerTmpFile;
        
        // create a list for temporary files for splitting the main file
        List<File> files = new ArrayList<File>();

        // read the file, split it into temporary files and sort them
        try {
            
            String line =" ";
            int currentfile = 0;
            try {
                while (remainingLines > 0) {
                    
                    if (arrayLength > remainingLines) {
                        arrayLength = (int) remainingLines;
                        remainingLines = 0;
                    } else {
                        remainingLines = remainingLines - arrayLength;
                    }
                    String[] fileContent = new String[arrayLength];
                    
                    currentfile++;
                    System.out.print("\rWorking on temporary file"  + currentfile +" /" + nFiles +"  (reading)    " );
                    
                    int lineCount = 0;
                    while ((lineCount < arrayLength)) {
                        line = fbr.readLine();
                        fileContent[lineCount] = line;
                        lineCount++;
                    }
                    
                    System.out.print("\rWorking on temporary file"  + currentfile +" /" + nFiles +"  (sorting)    " );
                    
                    files.add(sortAndSave(fileContent, tmpdirectory));
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            fbr.close();
            //System.out.println();
        }
        
        return files;
    }
    
    public void sortFile(File inputFile, File outputFile, File tempFileDir) {
        try {
            
            // read input file and sort into smaller files
            BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile),"UTF-8"));
            List<File> files = sortInBatch(bf, tempFileDir);
            
            // merge small sorted files into big sorted file
            mergeSortedFiles(files, outputFile);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
