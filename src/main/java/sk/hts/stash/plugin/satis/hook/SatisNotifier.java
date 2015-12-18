package sk.hts.stash.plugin.satis.hook;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.stash.exception.CommandFailedException;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.server.ApplicationPropertiesService;
import com.atlassian.stash.scm.git.GitCommandBuilderFactory;
import com.atlassian.stash.scm.git.GitScm;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.math.BigInteger;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SatisNotifier
{
    public static final String SETTINGS_KEY = "sk.hts.stash.plugin.satis.hook.stash-satis";
    public static final String API_URL_KEY = "cp-url";

    public static final String REPOSITORY_EXTENSION = ".git";

    public static final String HTTP_POST = "post";
    public static final String HTTP_PUT = "put";

    protected final PluginSettings pluginSettings;
    protected final GitScm gitScm;
    protected ApplicationPropertiesService properties;

    protected static final Logger logger = LoggerFactory.getLogger(SatisNotifier.class);

    private final static String PACKAGE_FILE = "composer.json";
    private final static String PACKAGE_NAME_KEY = "name";

    /**
     * MD5 encoder
     *
     * @param input string to be hashed
     *
     * @return hashed string
     *
     * @throws NoSuchAlgorithmException exception
     */
    protected String md5(String input) throws NoSuchAlgorithmException {
        String result = input;

        if(input != null) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());

            BigInteger hash = new BigInteger(1, md.digest());

            result = hash.toString(16);
            while(result.length() < 32) {
                result = "0" + result;
            }
        }

        return result;
    }

    /**
     * Build repository URL from repository instance and hook settings
     *
     * @param repository repository instance
     *
     * @return repository URL
     *
     * @throws UnsupportedEncodingException exception
     */
    protected String buildRepositoryUrl(Repository repository) throws UnsupportedEncodingException {
        UriBuilder repositoryUrl = UriBuilder.fromUri(this.properties.getBaseUrl());

        repositoryUrl.path(repository.getProject().getKey())
                .path(repository.getSlug() + REPOSITORY_EXTENSION);

        return repositoryUrl.build().toASCIIString();
    }

    /**
     * Async response processing of API response
     *
     * @return callback for async processing
     */
    protected Callback<JsonNode> awaitResponse() {
        return new Callback<JsonNode>() {
            @Override
            public void completed(HttpResponse<JsonNode> httpResponse) {
                logger.info("Got response: " + httpResponse.getBody().toString());

                if (httpResponse.getStatus() == Response.Status.OK.getStatusCode()) {
                    logger.info("HTTP request was accepted, repository should be updated shortly");
                } else {
                    logger.error("HTTP request ended with unexpected " +
                            "status code (" + httpResponse.getStatus() + ").");
                }
            }

            @Override
            public void failed(UnirestException e) {
                logger.error("HTTP request has failed: (" + e.getMessage() +")");
            }

            @Override
            public void cancelled() {
                logger.warn("HTTP request was cancelled.");
            }
        };
    }

    /**
     * Checks with API if currently active repository is registered
     *
     * @param repository currently active repository
     * @return boolean
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws UnirestException
     */
    protected boolean repositoryExists(Repository repository) throws UnsupportedEncodingException,
            NoSuchAlgorithmException, UnirestException
    {
        String satisUrl = this.getApiUrl();

        if (satisUrl.endsWith("/")) {
            satisUrl = satisUrl.substring(0, satisUrl.length() - 1);
        }

        satisUrl += "/{repositoryId}";

        HttpRequest request = Unirest.get(satisUrl)
                .routeParam("repositoryId", this.md5(this.buildRepositoryUrl(repository)));

        HttpResponse<JsonNode> httpResponse = request.asJson();

        return httpResponse.getStatus() == Response.Status.OK.getStatusCode();
    }

    /**
     * Pull package name from composer.json
     *
     * @param repository currently active repository
     * @return composer package name
     */
    protected String getPackageName(Repository repository) {
        GitCommandBuilderFactory builderFactory = this.gitScm.getCommandBuilderFactory();

        String packageName = "";
        try {
            String packageJson = builderFactory.builder(repository).
                    command("show").
                    argument("HEAD:" + PACKAGE_FILE)
                    .build(new StringOutputHandler())
                    .call();

            if (packageJson != null) {
                try {
                    JsonObject json = new JsonParser().parse(packageJson).getAsJsonObject();
                    packageName = json.get(PACKAGE_NAME_KEY).getAsString();
                } catch (JsonIOException | JsonSyntaxException e) {
                    logger.error("Unable to parse " + PACKAGE_FILE + ". (" + e.getMessage() + ").");
                }
            }

        } catch(CommandFailedException e) {
            logger.error("Unable to fetch " + PACKAGE_FILE + " contents with git: (" + e.getMessage() + ")");
        }

        return packageName;
    }

    /**
     * Constructor
     *
     * @param pluginSettingsFactory stash settings factory instance
     * @param properties hook settings
     * @param gitScm git
     */
    public SatisNotifier(final PluginSettingsFactory pluginSettingsFactory,
                        ApplicationPropertiesService properties,
                        GitScm gitScm
    ) {
        this.properties = properties;
        this.gitScm = gitScm;

        pluginSettings = pluginSettingsFactory.createSettingsForKey(SETTINGS_KEY);
    }

    /**
     * Satis control panel URL getter
     *
     * @return control panel URL
     */
    public String getApiUrl() {
        Object apiUrl = pluginSettings.get(API_URL_KEY);

        return (apiUrl instanceof String ? (String) apiUrl : "");
    }

    /**
     * Satis control panel URL setter
     *
     * @param apiUrl - control panel URL
     */
    public void setApiUrl(String apiUrl) {
        if (StringUtils.isBlank(apiUrl)) {
            pluginSettings.remove(API_URL_KEY);
        } else {
            pluginSettings.put(API_URL_KEY, apiUrl);
        }
    }

    /**
     * Creates HTTP DELETE request to control panel API
     *
     * @param repository stash repository
     */
    public void deleteRepository(Repository repository) {
        String satisUrl = this.getApiUrl();

        if (satisUrl.endsWith("/")) {
            satisUrl = satisUrl.substring(0, satisUrl.length() - 1);
        }

        satisUrl += "/{repositoryId}";

        logger.info("Requesting: " + satisUrl);

        try {
            Unirest.delete(satisUrl)
                    .routeParam("repositoryId", this.md5(this.buildRepositoryUrl(repository)))
                    .asJsonAsync(this.awaitResponse());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error("Unable to build repository ID: (" + e.getMessage() + ")");
        }
    }

    /**
     * Creates HTTP POST request to create repository in satis control panel
     *
     * @param repository stash repository
     * @param disableBuild build activation
     */
    public void updateRepository(Repository repository, boolean disableBuild) {
        String satisUrl = this.getApiUrl();

        if (satisUrl.isEmpty()) {
            logger.error("Satis API endpoint URL is empty. Did you forget to set it?");

            return;
        }

        String packageName = this.getPackageName(repository);

        if (packageName.isEmpty()) {
            logger.error("Composer package name was not found in " + PACKAGE_FILE + ".");

            return;
        }

        logger.info("Got package name - " + packageName);
        logger.info("Requesting: " + satisUrl);

        try {
            String httpMethod = HTTP_POST;

            if (this.repositoryExists(repository)) {
                if (satisUrl.endsWith("/")) {
                    satisUrl = satisUrl.substring(0, satisUrl.length() - 1);
                }

                satisUrl += "/{repositoryId}";
                httpMethod = HTTP_PUT;
            }

            HttpRequestWithBody request;
            if (httpMethod.equals(HTTP_PUT)) {
                request = Unirest.put(satisUrl);
                request.routeParam("repositoryId", this.md5(this.buildRepositoryUrl(repository)));
            } else {
                request = Unirest.post(satisUrl);
            }

            request.field("url", this.buildRepositoryUrl(repository))
                    .field("package_name", packageName)
                    .field("disable_build", disableBuild)
                    .asJsonAsync(this.awaitResponse());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error("Unable to build repository URL: (" + e.getMessage() + ")");
        } catch (UnirestException e) {
            logger.error("Unable create request to API: (" + e.getMessage() + ")");
        }
    }

    /**
     * Creates HTTP POST request to update repository in satis control panel
     *
     * @param oldRepository original repository before changes
     * @param newRepository repository after changes
     */
    public void updateRepository(Repository oldRepository, Repository newRepository) {
        String satisUrl = this.getApiUrl();
        String packageName = this.getPackageName(newRepository);

        if (packageName.isEmpty()) {
            logger.error("Composer package name was not found in " + PACKAGE_FILE + ".");

            return;
        }

        logger.info("Got package name - " + packageName);
        logger.info("Requesting: " + satisUrl);

        try {
            String httpMethod = HTTP_POST;

            if (this.repositoryExists(newRepository)) {
                logger.warn("Stopping request because of possible collision with existing repository, " +
                        "whos ID is already registered in control panel.");
            }

            if (this.repositoryExists(oldRepository)) {
                if (satisUrl.endsWith("/")) {
                    satisUrl = satisUrl.substring(0, satisUrl.length() - 1);
                }

                satisUrl += "/{repositoryId}";
                httpMethod = HTTP_PUT;
            }

            HttpRequestWithBody request;
            if (httpMethod.equals(HTTP_PUT)) {
                request = Unirest.put(satisUrl);
                request.routeParam("repositoryId", this.md5(this.buildRepositoryUrl(oldRepository)));
            } else {
                request = Unirest.post(satisUrl);
            }

            request.field("url", this.buildRepositoryUrl(newRepository))
                    .field("disable_build", false)
                    .asJsonAsync(this.awaitResponse());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            logger.error("Unable to build repository URL: (" + e.getMessage() + ")");
        } catch (UnirestException e) {
            logger.error("Unable create request to API: (" + e.getMessage() + ")");
        }
    }
}