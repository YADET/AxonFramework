/*
 * Copyright (c) 2010-2016. Axon Framework
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventstore;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventsourcing.DomainEventMessage;
import org.axonframework.serializer.Serializer;
import org.axonframework.upcasting.SimpleUpcasterChain;
import org.axonframework.upcasting.UpcasterChain;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Rene de Waele
 */
public abstract class AbstractEventStorageEngine implements EventStorageEngine {

    private Serializer serializer;
    private UpcasterChain upcasterChain = SimpleUpcasterChain.EMPTY;

    protected Serializer serializer() {
        return serializer;
    }

    protected UpcasterChain upcasterChain() {
        return upcasterChain;
    }

    @Override
    public Stream<? extends TrackedEventMessage<?>> readEvents(TrackingToken trackingToken) {
        return readEventData(trackingToken)
                .flatMap(entry -> EventUtils.upcastAndDeserialize(entry, serializer, upcasterChain, false).stream());
    }

    @Override
    public Stream<? extends DomainEventMessage<?>> readEvents(String aggregateIdentifier, long firstSequenceNumber) {
        return readEventData(aggregateIdentifier, firstSequenceNumber)
                .flatMap(entry -> EventUtils.upcastAndDeserialize(entry, serializer, upcasterChain, false).stream());
    }

    @Override
    public Optional<DomainEventMessage<?>> readSnapshot(String aggregateIdentifier) {
        return Optional.ofNullable(readSnapshotData(aggregateIdentifier))
                .map(entry -> EventUtils.upcastAndDeserialize(entry, serializer, upcasterChain, false).stream()
                        .findFirst().orElse(null));
    }

    @Override
    public void appendEvents(List<? extends EventMessage<?>> events) {
        appendEvents(events, serializer);
    }

    @Override
    public void storeSnapshot(DomainEventMessage<?> snapshot) {
        storeSnapshot(snapshot, serializer);
    }

    protected abstract void appendEvents(List<? extends EventMessage<?>> events, Serializer serializer);

    protected abstract void storeSnapshot(DomainEventMessage<?> snapshot, Serializer serializer);

    protected abstract Stream<SerializedDomainEventData<?>> readEventData(String identifier, long firstSequenceNumber);

    protected abstract Stream<SerializedTrackedEventData<?>> readEventData(TrackingToken trackingToken);

    protected abstract SerializedDomainEventData<?> readSnapshotData(String aggregateIdentifier);

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Sets the UpcasterChain which allow older revisions of serialized objects to be deserialized.
     *
     * @param upcasterChain the upcaster chain providing the upcasting capabilities
     */
    public void setUpcasterChain(UpcasterChain upcasterChain) {
        this.upcasterChain = upcasterChain;
    }

}