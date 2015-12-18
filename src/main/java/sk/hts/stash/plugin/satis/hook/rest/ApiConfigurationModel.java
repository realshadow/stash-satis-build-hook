package sk.hts.stash.plugin.satis.hook.rest;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize
public class ApiConfigurationModel {

    public String apiUrl;

    /**
     * Constructor
     *
     * @param apiUrl Satis control panel URL
     */
    public ApiConfigurationModel(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public ApiConfigurationModel() {
    }

    /**
     * Satis control panel URL getter
     *
     * @return Satis control panel URL
     */
    public String getApiUrl() {
        return this.apiUrl;
    }

    /**
     * Satis control panel URL setter
     *
     * @param apiUrl Satis control panel URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}