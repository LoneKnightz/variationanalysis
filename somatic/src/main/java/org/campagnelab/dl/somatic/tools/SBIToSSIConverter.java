package org.campagnelab.dl.somatic.tools;

import org.campagnelab.dl.framework.tools.arguments.AbstractTool;
import org.campagnelab.dl.somatic.storage.RecordReader;
import org.campagnelab.dl.varanalysis.protobuf.BaseInformationRecords;
import org.campagnelab.dl.varanalysis.protobuf.SegmentInformationRecords;
import org.campagnelab.goby.baseinfo.SequenceSegmentInformationWriter;

import java.io.File;
import java.io.IOException;

/**
 * Tool to convert from SBI to SSI format.
 * 
 * @author manuele
 */
public class SBIToSSIConverter extends AbstractTool<SBIToSSIConverterArguments> {

    SequenceSegmentInformationWriter writer;

    final SegmentHolder currentSegment = new SegmentHolder();

    @Override
    public SBIToSSIConverterArguments createArguments() {
        return new SBIToSSIConverterArguments();
    }

    @Override
    public void execute() {
        if (args().inputFile.isEmpty()) {
            System.err.println("You must provide input SBI files.");
        }
        int gap = args().gap;
        try {
            RecordReader sbiReader = new RecordReader(new File(args().inputFile).getAbsolutePath());

            sbiReader.forEach(sbiRecord -> {
                if (sbiRecord == null) {
                    closeOutput();
                } else {
                    manageRecord(sbiRecord, gap);
                }
            });
        } catch (IOException e) {
            System.err.println("Failed to parse " + args().inputFile);
            e.printStackTrace();
        }
        
    }

    private void manageRecord(BaseInformationRecords.BaseInformation record, int gap) {
        int position = record.getPosition();
        if (position - currentSegment.getCurrentLastLocation() > gap) {
           currentSegment.newSegment(record);
        }
        currentSegment.add(record);
    }

    private void closeOutput() {
       currentSegment.close();
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to close the SSI file");
            e.printStackTrace();
        }
    }

    class SegmentHolder {
        private SegmentInformationRecords.SegmentInformation.Builder builder =
                SegmentInformationRecords.SegmentInformation.newBuilder();
        private int currentLastPosition = 0;
        private int currentLastReferenceIndex = 0;
        private String currentLastReferenceId = "";

        /**
         * Creates a new segment and eventually closes the previous one.
         * @param first the first record of the new segment 
         */
        protected void newSegment(BaseInformationRecords.BaseInformation first) {
            this.close();
            builder = SegmentInformationRecords.SegmentInformation.newBuilder();
            SegmentInformationRecords.ReferencePosition.Builder refBuilder = SegmentInformationRecords.ReferencePosition.newBuilder();
            refBuilder.setLocation(first.getPosition());
            refBuilder.setReferenceIndex(first.getReferenceIndex());
            refBuilder.setReferenceId(first.getReferenceId());
            builder.setStartPosition(refBuilder);
            setAsLast(first);
        }

        protected void close() {
            if (builder != null) {
                //close the previous segment
                try {
                    writer.appendEntry(builder.build());
                    //set the current* as end position
                    SegmentInformationRecords.ReferencePosition.Builder refBuilder = SegmentInformationRecords.ReferencePosition.newBuilder();
                    refBuilder.setLocation(currentLastPosition);
                    refBuilder.setReferenceIndex(currentLastReferenceIndex);
                    refBuilder.setReferenceId(currentLastReferenceId);
                    builder.setEndPosition(refBuilder);
                } catch (IOException e) {
                    System.err.println("Unable to close the previous segment");
                    e.printStackTrace();
                } finally {
                    builder = null;
                    currentLastPosition = 0;
                    currentLastReferenceId = "";
                    currentLastReferenceIndex = 0;
                }
            }
        }

        public int getCurrentLastLocation() {
            return currentLastPosition;
        }

        /**
         * Adds a record to the current segment
         * @param record
         */
        public void add(BaseInformationRecords.BaseInformation record) {
            record.getSamplesList().forEach(sampleInfo -> {
                    SegmentInformationRecords.Sample.Builder sampleBuilder = SegmentInformationRecords.Sample.newBuilder();
                    SegmentInformationRecords.Base.Builder baseBuilder = SegmentInformationRecords.Base.newBuilder();
                    //TODO: set real values here
                    baseBuilder.addFeatures(1f);
                    baseBuilder.addLabels(2f);
                    baseBuilder.addTrueLabel("foo");
                    sampleBuilder.addBase(baseBuilder);
                    builder.addSample(sampleBuilder);
                }
            );
            SegmentInformationRecords.ReferencePosition.Builder refBuilder = SegmentInformationRecords.ReferencePosition.newBuilder();
            refBuilder.setLocation(record.getPosition());
            refBuilder.setReferenceIndex(record.getReferenceIndex());
            refBuilder.setReferenceId(record.getReferenceId());
            setAsLast(record);
        }

        /**
         * Sets the record as the last one in the current segment.
         * @param record
         */
        private void setAsLast(BaseInformationRecords.BaseInformation record) {
            currentLastPosition = record.getPosition();
            currentLastReferenceIndex = record.getReferenceIndex();
            currentLastReferenceId = record.getReferenceId();
        }
    }


}
