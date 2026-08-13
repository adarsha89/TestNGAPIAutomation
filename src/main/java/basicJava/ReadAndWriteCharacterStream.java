package basicJava;

import java.io.*;

public class ReadAndWriteCharacterStream {
    /*public static void main(String[] args) throws IOException {
        String file1 = "src/test/resources/data/bytesInput.txt";
        String file2 = "src/test/resources/data/bytesOutput.txt";
        FileReader fileReader1 = null;
        FileWriter fileWriter = null;
        try{
            fileReader1 = new FileReader(file1);
            fileWriter = new FileWriter(file2);
            int c = 0;
            while((c = fileReader1.read()) != -1){
                fileWriter.write(c);
            }
        }finally {
            if(fileReader1 != null){
                fileReader1.close();
            }
            if(fileWriter != null){
                fileWriter.close();
            }
        }
    }*/

    public static void main(String[] args) throws IOException {
        String file1 = "src/test/resources/data/bytesInput.txt";
        String file2 = "src/test/resources/data/bytesOutput.txt";

        BufferedReader bufferedReader = null;
        PrintWriter printWriter = null;

        try {
            bufferedReader= new BufferedReader(new FileReader(file1));
            printWriter = new PrintWriter(new File(file2));
            String l;

            while( (l = bufferedReader.readLine()) != null){
                printWriter.write(l);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(bufferedReader != null){
                bufferedReader.close();
            }
            if(printWriter != null){
                printWriter.close();
            }
        }

    }
}
