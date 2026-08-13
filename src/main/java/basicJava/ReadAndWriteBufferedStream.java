package basicJava;

import java.io.*;

public class ReadAndWriteBufferedStream {

    public static void main(String[] args) throws IOException {
        String file1 = "src/test/resources/data/bytesInput.txt";
        String file2 = "src/test/resources/data/bytesOutput.txt";

        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;

        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        try {
            fileInputStream= new FileInputStream(file1);
            fileOutputStream = new FileOutputStream(file2);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            bufferedInputStream.transferTo(bufferedOutputStream);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(bufferedInputStream != null){
                bufferedInputStream.close();
            }
            if(bufferedOutputStream != null){
                bufferedOutputStream.close();
            }
        }

    }
}
