package uk.ac.eeci;

import io.improbable.scienceos.Conductor;
import io.improbable.scienceos.Reference;
import io.improbable.scienceos.WorkerPool;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.LoggerContext;


public class CommandLineInterface {

    private final static String TOOL_NAME = "energy-agents";
    private final static Logger logger = LogManager.getLogger("uk.ac.eeci.energyagents");
    private final static String TMP_FILE_APPENDER_NAME = "TempFile";

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
            formatter.printHelp(TOOL_NAME, options);

            System.exit(1);
            return;
        }

        CommandLineInterface cli = new CommandLineInterface();
        cli.inputFilePath = cmd.getOptionValue("input");
        cli.outputFilePath = cmd.getOptionValue("output");
        cli.run();
    }

    private void run() {
        logger.info(String.format("Hi there. This is %s version %s.", TOOL_NAME, CitySimulation.inferModelVersion()));
        logTempFileName();
        Reference.pool.setCurrentExecutor(Reference.pool.main); // FIXME shouldnt be here
        logger.info(String.format("Attempting to read scenario description from file %s.", this.inputFilePath));
        CitySimulation citySimulation = ScenarioBuilder.readScenario(this.inputFilePath, this.outputFilePath);
        logger.info("Start of the simulation.");
        new Conductor(citySimulation).run();
        logger.info("Simulation terminated gracefully.");
    }

    private static void logTempFileName() {
        final LoggerContext ctx = LoggerContext.getContext(false);
        Configuration config = ctx.getConfiguration();
        FileAppender fileAppender = config.getAppender(TMP_FILE_APPENDER_NAME);
        if (fileAppender != null) {
            String logFileName = fileAppender.getFileName();
            logger.info(String.format("A detailed log is in %s.", logFileName));
        }
    }
}
