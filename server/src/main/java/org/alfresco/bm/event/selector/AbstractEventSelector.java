package org.alfresco.bm.event.selector;

import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventProcessor;
import org.alfresco.bm.event.EventProcessorRegistry;
import org.alfresco.bm.event.selector.EventDataObject.STATUS;

/**
 * Abstract base class for event selectors. 
 *  
 * @author steveglover
 * @since 1.3
 */
public abstract class AbstractEventSelector implements EventSelector
{
    protected String name;
    protected EventProcessorRegistry registry;

    public AbstractEventSelector(String name, EventProcessorRegistry registry)
    {
        super();
        this.name = name;
        this.registry = registry;
    }
    
    public AbstractEventSelector(EventProcessorRegistry registry)
    {
        this(null, registry);
    }

    public String getName()
    {
        return name;
    }

    /**
     * Implemented by subclasses to select a successor event.
     * 
     * @param input         the input to the previous event
     * @param response      the response from the previous event
     * @return              the successor event details
     */
    protected abstract EventSuccessor next(Object input, Object response);

    @Override
    public Event nextEvent(Object input, Object response) throws Exception
    {
        Event nextEvent = null;

        if (size() > 0)
        {
            // Choose the next event processor
            EventSuccessor eventSuccessor = next(input, response);
            
            String nextEventName = eventSuccessor.getEventName();
            long nextEventDelay = eventSuccessor.getDelay();
            EventDataObject nextEventInput = null;
    
            if (nextEventName != null && !nextEventName.equals("") && !nextEventName.equalsIgnoreCase("noop"))
            {
                EventProcessor eventProcessor = (EventProcessor)registry.getProcessor(nextEventName);
                if (eventProcessor == null)
                {
                    throw new RuntimeException(
                            "Event selector contains unknown event mapping: " + nextEventName + "." +
                            "No further events will be published.");
                }
                else if (!(eventProcessor instanceof EventDataCreator))
                {
                    // The next processor wired in cannot create its own input data.
                    // We assume that the event data can be passed directly in.
                    nextEventInput = new EventDataObject(STATUS.SUCCESS, response);
                }
                else
                {
                    EventDataCreator eventDataCreator = (EventDataCreator)eventProcessor;
                    nextEventInput = eventDataCreator.createDataObject(input, response);
                }
            }

            if(nextEventInput != null && nextEventInput.getStatus().equals(EventDataObject.STATUS.SUCCESS))
            {
                // Construct the event with the new data and an appropriate delay
                nextEvent = new Event(
                        nextEventName,
                        System.currentTimeMillis() + nextEventDelay,
                        nextEventInput.getData(), true);
            }
        }

        // Done
        return nextEvent;
    }    
}