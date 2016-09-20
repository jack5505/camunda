package org.camunda.tngp.transport.impl.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.ManyToOneConcurrentArrayQueue;
import org.camunda.tngp.dispatcher.Subscription;
import org.camunda.tngp.transport.impl.TransportChannelImpl;
import org.camunda.tngp.transport.impl.TransportContext;

public class Sender implements Agent, Consumer<SenderCmd>
{
    protected final ManyToOneConcurrentArrayQueue<SenderCmd> cmdQueue;

    protected final Int2ObjectHashMap<TransportChannelImpl> channelMap;

    protected final List<TransportChannelImpl> channelsWithControlFrames;

    protected final Subscription senderSubscription;

    protected final SenderBlockPeek blockPeek = new SenderBlockPeek();

    public Sender(TransportContext context)
    {
        cmdQueue = context.getSenderCmdQueue();
        senderSubscription = context.getSenderSubscription();
        channelMap = new Int2ObjectHashMap<>();
        channelsWithControlFrames = new ArrayList<>(10);
    }

    public int doWork() throws Exception
    {
        int workCount = 0;

        workCount += cmdQueue.drain(this);

        workCount += sendControlFrames();

        workCount += blockPeek.peek(senderSubscription, channelMap);
        workCount += blockPeek.doSend();

        return workCount;
    }

    protected int sendControlFrames()
    {
        int workCount = 0;

        for (int i = 0; i < channelsWithControlFrames.size(); i++)
        {
            channelsWithControlFrames.get(i).writeControlFrame();
            workCount++;
        }

        channelsWithControlFrames.clear();

        return workCount;
    }

    public String roleName()
    {
        return "sender";
    }

    @Override
    public void accept(SenderCmd t)
    {
        t.execute(this);
    }

    public void registerChannel(TransportChannelImpl c, CompletableFuture<Void> future)
    {
        channelMap.put(c.getId(), c);
        future.complete(null);
    }

    public void removeChannel(TransportChannelImpl c)
    {
        channelMap.remove(c.getId());
        channelsWithControlFrames.remove(c);
        blockPeek.onChannelRemoved(c);
    }

    public void sendControlFrame(TransportChannelImpl channel)
    {
        channelsWithControlFrames.add(channel);
    }

}
