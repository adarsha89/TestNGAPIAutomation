package basicJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/* Should be used for most primitive i/o*/
public class ReadAndWriteByteStream {

    public static void main(String[] args) throws IOException {
        String file1 = "src/test/resources/data/bytesInput.txt";
        String file2 = "src/test/resources/data/bytesOutput.txt";
        FileInputStream fis1 = null;
        FileOutputStream fis2 = null;
        try {
            fis1 = new FileInputStream(file1);
            fis2 = new FileOutputStream(file2);
            int c;
            while((c = fis1.read()) != -1){
                fis2.write(c);
            }
        }
        finally {
            if(fis1 != null){
                fis1.close();
            }
            if(fis2 != null){
                fis2.close();
            }
        }


    }
}
