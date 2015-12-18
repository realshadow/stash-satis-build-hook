package sk.hts.stash.plugin.satis.hook;

import java.io.IOException;

import org.slf4j.Logger;

import com.atlassian.stash.io.LineReader;
import com.atlassian.stash.io.LineReaderOutputHandler;
import com.atlassian.stash.scm.CommandOutputHandler;
import com.atlassian.utils.process.Watchdog;
import org.slf4j.LoggerFactory;

class StringOutputHandler extends LineReaderOutputHandler implements CommandOutputHandler<String> {
    protected static final Logger logger = LoggerFactory.getLogger(SatisNotifier.class);

    private final StringBuilder stringBuilder;
    private Watchdog watchdog;

    /**
     * Constructor
     */
    public StringOutputHandler() {
        super("UTF-8");

        stringBuilder = new StringBuilder();
    }

    /**
     * Gets command output
     *
     * @return command output
     */
    @Override
    public String getOutput() {
        return stringBuilder.toString();
    }

    /**
     * Sets watchdog
     *
     * @param watchdog watchdog
     */
    @Override
    public void setWatchdog(Watchdog watchdog) {
        this.watchdog = watchdog;
    }

    /**
     * Line for line processing of command output
     *
     * @param reader line reader
     */
    @Override
    public void processReader(LineReader reader) {
        try {
            String line;

            if (watchdog != null) {
                watchdog.resetWatchdog();
            }

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
        } catch (IOException e) {
            logger.error("Caught IOException while reading git command output", e);
        }
    }
}