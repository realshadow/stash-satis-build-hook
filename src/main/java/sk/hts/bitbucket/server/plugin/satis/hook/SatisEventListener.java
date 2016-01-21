package sk.hts.bitbucket.server.plugin.satis.hook;

import com.atlassian.event.api.EventListener;
import com.atlassian.bitbucket.event.repository.RepositoryDeletedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryModifiedEvent;

public class SatisEventListener {
    private final SatisNotifier satisNotifier;

    /**
     * Constructor
     *
     * @param satisNotifier - SatisNotifier instance
     */
    public SatisEventListener(SatisNotifier satisNotifier) {
        this.satisNotifier = satisNotifier;
    }

    /**
     * Catch and process delete event
     *
     * @param deletedEvent stash event
     */
    @EventListener
    public void onDelete(RepositoryDeletedEvent deletedEvent) {
        this.satisNotifier.deleteRepository(deletedEvent.getRepository());
    }

    /**
     * Catch and process change event (move, rename)
     *
     * @param modifiedEvent stash event
     */
    @EventListener
    public void onChange(RepositoryModifiedEvent modifiedEvent) {
        if (modifiedEvent.isNameChanged() || modifiedEvent.isSlugChanged()) {
            this.satisNotifier.updateRepository(modifiedEvent.getOldValue(), modifiedEvent.getRepository());
        }
    }
}