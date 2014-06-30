package gov.nysenate.openleg;

import gov.nysenate.util.Config;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * The Environment class is essentially a configuration for an OpenLeg workspace.
 * Each workspace contains its own set of directories to store the source data files
 * as well as an associated database schema to store the data.
 *
 * Only one environment can be active at a given time but having multiple available
 * can be beneficial for testing new features on a subset of bills for example.
 */
public class Environment
{
    public final static String DEFAULT_SCHEMA = "master";

    private int id;
    private String schema = DEFAULT_SCHEMA;
    private File baseDirectory;
    private File stagingDirectory;
    private File workingDirectory;
    private File archiveDirectory;
    private boolean active;
    private Date createdDateTime;
    private Date modifiedDateTime;

    public Environment() {}

    public Environment(String directoryPath) {
        this(new File(directoryPath));
    }

    public Environment(Config config, String prefix, String schema) {
        this.schema = schema;
        this.active = true;
        this.baseDirectory = new File(config.getValue(prefix+".directory"));
        this.stagingDirectory = new File(config.getValue(prefix+".data"));
        this.workingDirectory = new File(config.getValue(prefix+".work"));
        this.archiveDirectory = new File(config.getValue(prefix+".archive"));
    }

    public Environment(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.stagingDirectory = new File(baseDirectory,"data");
        this.workingDirectory = new File(baseDirectory,"work");
        this.archiveDirectory = new File(baseDirectory,"archive");
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public File getStagingDirectory() {
        return stagingDirectory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public File getArchiveDirectory() {
        return archiveDirectory;
    }

    public int getId() {
        return id;
    }

    public String getSchema() {
        return schema;
    }

    public boolean isActive() {
        return active;
    }

    public Date getCreatedDateTime() {
        return createdDateTime;
    }

    public Date getModifiedDateTime() {
        return modifiedDateTime;
    }

    /** TODO: Move this to the DAO */
    public void create() throws IOException {
        FileUtils.forceMkdir(baseDirectory);
        FileUtils.forceMkdir(stagingDirectory);
        FileUtils.forceMkdir(workingDirectory);
        FileUtils.forceMkdir(archiveDirectory);
    }

    /** TODO: Move this to the DAO */
    public void delete() throws IOException {
        FileUtils.deleteQuietly(baseDirectory);
    }

    /** TODO: Move this to the DAO */
    public void reset() throws IOException {
        delete();
        create();
    }
}
