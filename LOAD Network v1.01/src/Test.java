import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Test {
    public static void main(String[] args){
        String a[] = IntStream.range(1789, 2000).mapToObj(String::valueOf).toArray(String[]::new);
        System.out.println("AAAAAA");
    }
}