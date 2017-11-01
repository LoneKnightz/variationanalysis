package org.campagnelab.dl.genotype.segments.splitting;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.campagnelab.dl.genotype.segments.Segment;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;

import java.util.Collections;
import java.util.Iterator;


/**
 * Split result of the {@link SingleCandidateIndelSplitStrategy}
 */
public class SingleCandidateIndelSegment extends Segment {
    private final String candidateReferenceId;
    private int candidateIndelPosition;
    private final int startPosition;
    private int endPosition;
    private final int windowSize;
    private final Segment parent;
    private boolean closed = false;

    protected SingleCandidateIndelSegment(final SingleCandidateIndelSplitStrategy.BasePositionList beforePositions,
                                          final Segment parent, final BaseInformationRecords.BaseInformation indel,
                                          int windowSize) {
        super(parent.fillInFeatures);
        this.parent = parent;
        this.candidateIndelPosition = indel.getPosition();
        this.candidateReferenceId = indel.getReferenceId();
        this.windowSize = windowSize;
        startPosition = this.detectStartPosition(beforePositions);
        this.add(indel);
        BaseInformationRecords.BaseInformation base = parent.getRecordAt(startPosition);
        if (base == null) {
            System.out.println("Null start base???");
        }
        this.setAsFirst(parent.getRecordAt(startPosition));
    }

    /**
     * Detects the start position of this segment.
     *
     * @param beforePositions
     * @return
     */
    private int detectStartPosition(SingleCandidateIndelSplitStrategy.BasePositionList beforePositions) {
        for (int position : beforePositions) {
            //the first one in the window
            if (this.candidateIndelPosition - position <= this.windowSize
                    && this.parent.getFirstPosition() <= position) {
                return position;
            }
        }
        return this.candidateIndelPosition;
    }

    @Override
    public void add(final BaseInformationRecords.BaseInformation base) {
        if (this.accept(base)) {
            this.endPosition = base.getPosition();
            this.setAsLast(base);
        }
    }

    /**
     * Returns the complete list of records, including those interleaved with genomic positions (for insertion/deletion).
     *
     * @return
     */
    @Override
    public Iterable<BaseInformationRecords.BaseInformation> getAllRecords() {
        return getAllRecords(startPosition, endPosition);

    }

    /**
     * Returns the complete list of records, including those interleaved with genomic positions (for insertion/deletion).
     *
     * @return
     */
    @Override
    public Iterable<BaseInformationRecords.BaseInformation> getAllRecords(int startPosition, int endPosition) {
        ObjectArrayList<BaseInformationRecords.BaseInformation> list = new ObjectArrayList(this.actualLength() * 3 / 2);
        for (BaseInformationRecords.BaseInformation record : parent.recordList) {
            if (record.getPosition() >= startPosition && record.getPosition() <= endPosition) {
                if (!parent.recordList.hideSet.contains(record)) {
                    list.add(record);
                }
                list.addAll(parent.recordList.afterRecord.getOrDefault(record, Collections.emptyList()));
            }
        }

        return list;
    }
    @Override
    public BaseInformationRecords.BaseInformation getFirstRecord() {
        return parent.getRecordAt(startPosition);
    }

    /**
     * Decides if the base belongs to this subsegment
     *
     * @param base
     * @return
     */
    private boolean accept(BaseInformationRecords.BaseInformation base) {
        if (base.getPosition() >= this.candidateIndelPosition)
            return base.getPosition() - this.candidateIndelPosition <= this.windowSize;
        else
            return this.candidateIndelPosition - base.getPosition() < this.windowSize;

    }

    /**
     * Checks if the subsegment has reached the window size
     *
     * @return
     */
    protected boolean isOpen() {
        return (this.getLastPosition() - this.candidateIndelPosition <= this.windowSize) && !closed;
    }


    public String getIndelReferenceId() {
        return this.candidateReferenceId;
    }

    public int getIndelPosition() {
        return this.candidateIndelPosition;
    }

    public int actualLength() {
        return parent.actualLength(startPosition, endPosition);

    }

    /**
     * Extends this segment considering the new indel position
     *
     * @param position the position of the last indel
     */
    public void newIndel(int position) {
        //this.endPosition = position + windowSize;
        this.candidateIndelPosition = position;
    }

    public void forceClose() {
        this.closed = true;
    }
}
