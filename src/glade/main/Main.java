// Copyright 2015-2016 Stanford University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package glade.main;

import glade.constants.Files;
import glade.grammar.fuzz.GrammarFuzzer.SampleParameters;
import glade.main.Settings.FuzzSettings;
import glade.main.Settings.Fuzzer;
import glade.main.Settings.GrammarSettings;
import glade.main.Settings.Program;
import glade.main.Settings.ProgramSettings;
import glade.util.Log;

import java.util.*;

public class Main {
    public static GrammarSettings getDefaultGrammarSettings() {
        return new GrammarSettings(Files.GRAMMAR_PATH);
    }

    public static SampleParameters getDefaultSampleParameters() {
        return new SampleParameters(new double[]{0.2, 0.2, 0.2, 0.4}, 0.8, 0.1, 100);
    }

    public static FuzzSettings getDefaultFuzzSettings(Random random, Fuzzer fuzzer) {
        return new FuzzSettings(20, 10, 1000, getDefaultSampleParameters(), fuzzer);
    }

    private static boolean runTest(ProgramSettings program, String example) {
        if (ProgramDataUtils.getQueryOracle(program.data).query(example)) {
            Log.info("TEST PASSED!");
            return true;
        } else {
            Log.info("TEST FAILED!");
            Log.info("ERROR:");
            Log.info(program.data.getOracle().execute(example));
            return false;
        }
    }

    public static class IntPair {
        public final int pass;
        public final int fail;

        public IntPair(int pass, int fail) {
            this.pass = pass;
            this.fail = fail;
        }
    }

    public static IntPair runTest(ProgramSettings program) {
        Log.info("TESTING PROGRAM: " + program.name);
        int pass = 0;
        int fail = 0;
        for (String example : program.examples.getEmptyExamples()) {
            Log.info("TESTING EMPTY EXAMPLE:");
            Log.info(example);
            if (runTest(program, example)) {
                pass++;
            } else {
                fail++;
            }
        }
        for (String example : program.examples.getTrainExamples()) {
            Log.info("TESTING TRAINING EXAMPLE:");
            Log.info(example);
            if (runTest(program, example)) {
                pass++;
            } else {
                fail++;
            }
        }
        return new IntPair(pass, fail);
    }

    public static void runLearn(ProgramSettings program, GrammarSettings grammar) {
        GrammarDataUtils.learnAllGrammar(grammar.grammarPath, program.name, program.data, program.examples);
    }

    public static void runFuzz(ProgramSettings program, GrammarSettings grammar, FuzzSettings fuzz, Random random) {
        Iterable<String> samples = fuzz.fuzzer.getSamples(program, grammar, fuzz, random);
        int pass = 0;
        int count = 0;
        for (String sample : samples) {
            Log.info("SAMPLE:");
            Log.info(sample);
            if (ProgramDataUtils.getQueryOracle(program.data).query(sample)) {
                Log.info("PASS\n");
                pass++;
            } else {
                Log.info("FAIL\n");
            }
            count++;
            if (count >= fuzz.numIters) {
                break;
            }
        }
        Log.info("PASS RATE: " + (float) pass / fuzz.numIters);
    }

    public static void usage() {
        System.out.println("usage: java -jar glade.jar -mode [learn|fuzz|test] [-program [sed|grep|flex|xml|python|python-wrapped]] [-fuzzer [grammar|combined]] [-log <filename>] [-verbose]");
        System.out.println("note: -program option required if mode=learn or mode=fuzz");
        System.out.println("note: -fuzzer option required if mode=fuzz");
        System.out.println("note: -log defaults to log.txt");
        System.exit(0);
    }

    private static Program getProgram(String programName) {
        if (programName == null) {
            usage();
        }
        switch (programName) {
            case "sed":
                return Program.SED;
            case "grep":
                return Program.GREP;
            case "flex":
                return Program.FLEX;
            case "xml":
                return Program.XML;
            case "python":
                return Program.PYTHON;
            case "python-wrapped":
                return Program.PYTHON_WRAPPED;
            default:
                usage();
                return null;
        }
    }

    private static Fuzzer getFuzzer(String fuzzerName) {
        if (fuzzerName == null) {
            usage();
        }
        switch (fuzzerName) {
            case "grammar":
                return Fuzzer.GRAMMAR;
            case "combined":
                return Fuzzer.COMBINED;
            default:
                usage();
                return null;
        }
    }

    public static void run(String[] args) {
        String mode = null;
        String programName = null;
        String fuzzerName = null;
        String logName = null;
        boolean verbose = false;

        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "-mode":
                    if (mode != null) {
                        usage();
                    }
                    i++;
                    mode = args[i];
                    break;
                case "-program":
                    if (programName != null) {
                        usage();
                    }
                    i++;
                    programName = args[i];
                    break;
                case "-fuzzer":
                    if (fuzzerName != null) {
                        usage();
                    }
                    i++;
                    fuzzerName = args[i];
                    break;
                case "-log":
                    if (logName != null) {
                        usage();
                    }
                    i++;
                    logName = args[i];
                    break;
                case "-verbose":
                    verbose = true;
                    break;
                default:
                    usage();
                    break;
            }

            i++;
        }

        try {
            if (mode == null) {
                usage();
            }

            if (logName == null) {
                logName = "log.txt";
            }
            Log.init(logName, verbose);

            switch (mode) {
                case "test":
                    int pass = 0;
                    int fail = 0;
                    Collection<Program> programs = new ArrayList<>();
                    if (programName == null) {
                        programs.addAll(Arrays.asList(Program.SED, Program.GREP, Program.FLEX, Program.XML, Program.PYTHON, Program.PYTHON_WRAPPED));
                    } else {
                        programs.add(getProgram(programName));
                    }
                    Collection<String> passedPrograms = new ArrayList<>();
                    Collection<String> failedPrograms = new ArrayList<>();
                    for (Program program : programs) {
                        IntPair curResults = runTest(program.getSettings());
                        (curResults.fail == 0 ? passedPrograms : failedPrograms).add(program.getSettings().name);
                        pass += curResults.pass;
                        fail += curResults.fail;
                    }
                    int total = pass + fail;
                    Log.info("PASSED: " + pass + "/" + total);
                    Log.info("FAILED: " + fail + "/" + total);
                    Log.info("PROGRAMS PASSED:");
                    for (String passedProgram : passedPrograms) {
                        Log.info(passedProgram);
                    }
                    Log.info("PROGRAMS FAILED:");
                    for (String failedProgram : failedPrograms) {
                        Log.info(failedProgram);
                    }
                    break;
                case "learn": {
                    GrammarSettings grammarSettings = getDefaultGrammarSettings();
                    Program program = getProgram(programName);
                    long time = System.currentTimeMillis();
                    runLearn(program.getSettings(), grammarSettings);
                    Log.info("TOTAL TIME: " + ((System.currentTimeMillis() - time) / 1000.0) + " seconds");
                    break;
                }
                case "fuzz": {
                    Random random = new Random();
                    GrammarSettings grammarSettings = getDefaultGrammarSettings();
                    Program program = getProgram(programName);
                    Fuzzer fuzzer = getFuzzer(fuzzerName);
                    FuzzSettings fuzzerSettings = getDefaultFuzzSettings(random, fuzzer);
                    runFuzz(program.getSettings(), grammarSettings, fuzzerSettings, random);
                    break;
                }
                default:
                    usage();
                    break;
            }
        } catch (Exception e) {
            Log.err(e);
        }
    }

    public static void main(String... args) {
        run(args);
    }
}
