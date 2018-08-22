/*
 * Copyright (c) 2014-2018, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
 * (here and after addressed as Inventors)
 * All Rights Reserved
 *
 * Permission to use, copy, modify and distribute any part of this program for
 * educational, research and non-profit purposes, by non-profit institutions
 * only, without fee, and without a written agreement is hereby granted,
 * provided that the above copyright notice, this paragraph and the following
 * three paragraphs appear in all copies.
 *
 * Those desiring to incorporate this work into commercial products or use for
 * commercial purposes should contact MiLaboratory LLC, which owns exclusive
 * rights for distribution of this program for commercial purposes, using the
 * following email address: licensing@milaboratory.com.
 *
 * IN NO EVENT SHALL THE INVENTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 * SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 * ARISING OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE INVENTORS HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE SOFTWARE PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE INVENTORS HAS
 * NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 * MODIFICATIONS. THE INVENTORS MAKES NO REPRESENTATIONS AND EXTENDS NO
 * WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF THE SOFTWARE WILL NOT INFRINGE ANY
 * PATENT, TRADEMARK OR OTHER RIGHTS.
 */
package com.milaboratory.mixcr.cli;

import cc.redberry.primitives.Filter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.milaboratory.core.sequence.AminoAcidSequence;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.TranslationParameters;
import com.milaboratory.mixcr.basictypes.Clone;
import com.milaboratory.mixcr.basictypes.CloneSet;
import com.milaboratory.mixcr.basictypes.CloneSetIO;
import com.milaboratory.mixcr.export.InfoWriter;
import com.milaboratory.util.CanReportProgressAndStage;
import com.milaboratory.util.SmartProgressReporter;
import io.repseq.core.GeneFeature;
import io.repseq.core.VDJCLibraryRegistry;

import java.util.List;
import java.util.Objects;

public class ActionExportClones extends ActionExport<Clone> {
    public ActionExportClones(CloneExportParameters parameters) {
        super(parameters, Clone.class);
    }

    public ActionExportClones() {
        this(new CloneExportParameters());
    }

    @Override
    public void go0() throws Exception {
        CloneExportParameters parameters = (CloneExportParameters) this.parameters;
        try (InfoWriter<Clone> writer = new InfoWriter<>(parameters.getOutputFile())) {
            CloneSet set = CloneSetIO.read(parameters.getInputFile(), VDJCLibraryRegistry.getDefault());

            set = CloneSet.transform(set, parameters.getFilter());

            writer.attachInfoProviders((List) parameters.exporters);
            writer.ensureHeader();
            long limit = parameters.getLimit();
            for (int i = 0; i < set.size(); i++) {
                if (set.get(i).getFraction() < parameters.minFraction ||
                        set.get(i).getCount() < parameters.minCount) {
                    limit = i;
                    break;
                }
            }
            ExportClones exportClones = new ExportClones(set, writer, limit);
            SmartProgressReporter.startProgressReport(exportClones, System.err);
            exportClones.run();
        }
    }

    @Override
    public String command() {
        return "exportClones";
    }

    public static final class CFilter implements Filter<Clone> {
        final boolean filterOutOfFrames, filterStopCodons;

        public CFilter(boolean filterOutOfFrames, boolean filterStopCodons) {
            this.filterOutOfFrames = filterOutOfFrames;
            this.filterStopCodons = filterStopCodons;
        }

        @Override
        public boolean accept(Clone clone) {
            if (filterOutOfFrames) {
                NSequenceWithQuality cdr3 = clone.getFeature(GeneFeature.CDR3);
                if (cdr3 == null || cdr3.size() % 3 != 0)
                    return false;
            }

            if (filterStopCodons) {
                for (GeneFeature assemblingFeature : clone.getParentCloneSet().getAssemblingFeatures()) {
                    GeneFeature codingFeature = GeneFeature.getCodingGeneFeature(assemblingFeature);
                    if (codingFeature == null)
                        continue;

                    for (int i = 0; i < clone.numberOfTargets(); ++i) {
                        NSequenceWithQuality codingSeq = clone.getPartitionedTarget(i).getFeature(codingFeature);
                        if (codingSeq == null)
                            continue;
                        TranslationParameters tr = clone.getPartitionedTarget(i).getPartitioning().getTranslationParameters(codingFeature);
                        if (tr == null)
                            return false;
                        if (AminoAcidSequence.translate(codingSeq.getSequence(), tr).containStops())
                            return false;
                    }
                }
            }

            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CFilter)) return false;
            CFilter cFilter = (CFilter) o;
            return filterOutOfFrames == cFilter.filterOutOfFrames &&
                    filterStopCodons == cFilter.filterStopCodons;
        }

        @Override
        public int hashCode() {
            return Objects.hash(filterOutOfFrames, filterStopCodons);
        }
    }

    public static final class ExportClones implements CanReportProgressAndStage {
        final static String stage = "Exporting clones";
        final CloneSet clones;
        final InfoWriter<Clone> writer;
        final long size;
        volatile long current = 0;
        final long limit;

        private ExportClones(CloneSet clones, InfoWriter<Clone> writer, long limit) {
            this.clones = clones;
            this.writer = writer;
            this.size = clones.size();
            this.limit = limit;
        }

        @Override
        public String getStage() {
            return stage;
        }

        @Override
        public double getProgress() {
            return (1.0 * current) / size;
        }

        @Override
        public boolean isFinished() {
            return current == size;
        }

        void run() {
            for (Clone clone : clones.getClones()) {
                if (current == limit)
                    break;
                writer.put(clone);
                ++current;
            }
        }
    }

    @Parameters(commandDescription = "Export clones to tab-delimited text file")
    public static class CloneExportParameters extends ActionExportParameters<Clone> {
        @Parameter(description = "Exclude clones with out-of-frame clone sequences (fractions will be recalculated)",
                names = {"-o", "--filter-out-of-frames"})
        public boolean filterOutOfFrames = false;

        @Parameter(description = "Exclude sequences containing stop codons (fractions will be recalculated)",
                names = {"-t", "--filter-stops"})
        public boolean filterStops = false;

        @Parameter(description = "Filter clones by minimal clone fraction",
                names = {"-q", "--minimal-clone-fraction"})
        public float minFraction = 0;

        @Parameter(description = "Filter clones by minimal clone read count",
                names = {"-m", "--minimal-clone-count"})
        public long minCount = 0;

        public boolean getFilterOutOfFrames() {
            return filterOutOfFrames;
        }

        public boolean getFilterStops() {
            return filterStops;
        }

        @Override
        public Filter<Clone> getFilter() {
            final Filter<Clone> superFilter = super.getFilter();
            final CFilter cFilter = new CFilter(getFilterOutOfFrames(), getFilterStops());
            return new Filter<Clone>() {
                @Override
                public boolean accept(Clone object) {
                    if (!superFilter.accept(object))
                        return false;
                    return cFilter.accept(object);
                }
            };
        }
    }
}
