package basicJava;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpressions {
    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("[A-Za-z0-9+_.-]+@(.+)");
        Matcher matcher = pattern.matcher("test@example.com");
        if(matcher.matches()){
            System.out.println("Valid email");
        }
    }
}
