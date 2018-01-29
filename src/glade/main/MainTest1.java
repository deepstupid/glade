package glade.main;

public class MainTest1 {
    // -mode [learn|fuzz|test] [-program [sed|grep|flex|xml|python|python-wrapped]] [-fuzzer [grammar|combined]] [-log <filename>] [-verbose]

    public static void main(String[] args) {
        Main.main(
                "-mode test -program grep -fuzzer combined -verbose".split(" ")
        );
    }
}
