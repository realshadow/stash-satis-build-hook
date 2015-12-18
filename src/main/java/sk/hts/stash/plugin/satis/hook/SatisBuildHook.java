package sk.hts.stash.plugin.satis.hook;

import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import com.atlassian.stash.setting.*;
import com.atlassian.stash.repository.Repository;

import java.util.Collection;

public class SatisBuildHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator
{
    private final SatisNotifier satisNotifier;

    /**
     * Constructor
     *
     * @param satisNotifier SatisNotifier instance
     */
    public SatisBuildHook(SatisNotifier satisNotifier) {
        this.satisNotifier = satisNotifier;
    }

    /**
     * Catch and process push event
     *
     * @param context - hook context
     * @param refChanges - collection of changes
     */
    @Override
    public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
        this.satisNotifier.updateRepository(context.getRepository(), context.getSettings().getBoolean("disable_build"));
    }

    /**
     * Form settings validation
     *
     * @param settings hook settings
     * @param errors validation errors
     * @param repository stash repository instance
     */
    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {
        if (settings.getString("disable_build", "").isEmpty()) {
            errors.addFieldError("disable_build", "Please select one option.");
        }
    }
}