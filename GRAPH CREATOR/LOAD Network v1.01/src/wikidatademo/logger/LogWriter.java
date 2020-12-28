
package wikidatademo.logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LOAD graph for GUI Demo
 * 
 * Log Writer for the Demo
 * 
 * Originally published November 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class LogWriter {
    private BufferedWriter logwriter;           // buffered writer for the logfile
    private DateFormat dateFormat;              // format object for printing dates
    private String df = "yyyy-MM-dd HH:mm:ss";  // format scheme for printing the dates
    
    /**
     * Constructor that obtains relevant Settings from Settings object
     * @param S Settings object containing program settings
     */
    public LogWriter(String filename) {
        dateFormat = new SimpleDateFormat(df);
        try {
            // create new buffered writer for logfile and set to append
            logwriter = new BufferedWriter(new FileWriter(filename, true));
            logwriter.append(dateFormat.format(new Date()) + " >>> Start session <<<\n");
            logwriter.flush();
        } catch (Exception e) {
        }
    }
    
    /**
     * Flush contents of the buffer and close the file
     */
    public void closeFile() {
        try {
            logwriter.append(dateFormat.format(new Date()) + " >>> End session <<<\n\n"); 
            logwriter.close();
        } catch (Exception e) {
        }
    }
    
    /**
     * Write string to logfile
     * @param log String which is to be appended to the logfile
     */
    public void writeToFile(String logstring) {
        try {
            logwriter.append(dateFormat.format(new Date()) + " " + logstring +" \n");   
            logwriter.flush();
        } catch (Exception e) {
        }
    }
    
    /**
     * Write contents of an exception to logfile, including a stack trace
     * @param t Exception which is to be appended to the logfile
     */
    public void writeToFile(Throwable t) {
        String errstring = t.toString();
        for (StackTraceElement element : t.getStackTrace()) {   // for all elements in the stack trace
            errstring += "\n" + element.toString();             // append them to a single string
        }
        writeToFile(errstring);                                 // and write string to file
    }
    
}
