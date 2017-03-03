package uk.ac.eeci;

import io.improbable.scienceos.Conductor;
import io.improbable.scienceos.Reference;
import io.improbable.scienceos.WorkerPool;
import org.apache.commons.cli.*;

public class CommandLineInterface {

    private String inputFilePath;
    private String outputFilePath;

    public static void main(String ... args) {

        Options options = new Options();

        Option input = new Option("i", "input", true, "file path to scenario db");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "file path for output db");
        output.setRequired(true);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("energy-agents", options);

            System.exit(1);
            return;
        }

        CommandLineInterface cli = new CommandLineInterface();
        cli.inputFilePath = cmd.getOptionValue("input");
        cli.outputFilePath = cmd.getOptionValue("output");
        cli.run();
    }

    private void run() {
        Reference.pool = new WorkerPool(4); // FIXME shouldnt be here
        Reference.pool.setCurrentExecutor(Reference.pool.main); // FIXME shouldnt be here
        CitySimulation citySimulation = ScenarioBuilder.readScenario(this.inputFilePath, this.outputFilePath);
        new Conductor(citySimulation).run();
    }
}
