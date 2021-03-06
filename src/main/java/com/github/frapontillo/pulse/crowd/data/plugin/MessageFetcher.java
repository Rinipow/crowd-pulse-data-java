package com.github.frapontillo.pulse.crowd.data.plugin;

import com.github.frapontillo.pulse.crowd.data.entity.Message;
import com.github.frapontillo.pulse.spi.IPlugin;
import com.github.frapontillo.pulse.spi.PluginConfigHelper;
import com.github.frapontillo.pulse.util.PulseLogger;
import com.google.gson.JsonElement;
import com.github.frapontillo.pulse.crowd.data.repository.MessageRepository;
import org.apache.logging.log4j.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.observers.SafeSubscriber;

import java.util.Date;
import java.util.List;

/**
 * An implementation of {@link IPlugin} that, no matter the input stream, waits for its completion
 * and then emits all of the {@link Message}s stored in the database, eventually completing or
 * erroring.
 * <p/>
 * Use this plugin for transforming any stream into a stream containing all previously stored
 * {@link
 * Message}s.
 *
 * @author Francesco Pontillo
 */
public class MessageFetcher extends IPlugin<Object, Message, MessageFetcher.MessageFetcherOptions> {
    public final static String PLUGIN_NAME = "message-fetch";
    private MessageRepository messageRepository;
    private final Logger logger = PulseLogger.getLogger(MessageFetcher.class);

    @Override public String getName() {
        return PLUGIN_NAME;
    }

    public IPlugin<Object, Message, MessageFetcherOptions> getInstance() {
        return new MessageFetcher();
    }

    @Override public MessageFetcherOptions getNewParameter() {
        return new MessageFetcherOptions();
    }

    @Override
    protected Observable.Operator<Message, Object> getOperator(MessageFetcherOptions parameters) {
        // use a custom db, if any
        messageRepository = new MessageRepository(parameters.getDb());

        return subscriber -> new SafeSubscriber<>(new Subscriber<Object>() {
            @Override public void onCompleted() {
                // fetch all messages from the database and subscribe view the new subscriber
                Observable<Message> dbMessages = messageRepository
                        .find(parameters.getSince(), parameters.getUntil(),
                                parameters.getLanguages());
                dbMessages.subscribe(subscriber);
            }

            @Override public void onError(Throwable e) {
                e.printStackTrace();
                subscriber.onError(e);
            }

            @Override public void onNext(Object o) {
                // do absolutely nothing
            }
        });
    }

    /**
     * Fetching options that include the database name from {@link GenericDbConfig}.
     */
    public class MessageFetcherOptions extends GenericDbConfig<MessageFetcherOptions> {
        private Date since;
        private Date until;
        private List<String> languages;

        public Date getSince() {
            return since;
        }

        public void setSince(Date since) {
            this.since = since;
        }

        public Date getUntil() {
            return until;
        }

        public void setUntil(Date until) {
            this.until = until;
        }

        public List<String> getLanguages() {
            return languages;
        }

        public void setLanguages(List<String> languages) {
            this.languages = languages;
        }

        @Override public MessageFetcherOptions buildFromJsonElement(JsonElement json) {
            return PluginConfigHelper.buildFromJson(json, MessageFetcherOptions.class);
        }
    }

}
