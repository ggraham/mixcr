/*
 * Copyright (c) 2014-2015, Bolotin Dmitry, Chudakov Dmitry, Shugay Mikhail
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
 * commercial purposes should contact the Inventors using one of the following
 * email addresses: chudakovdm@mail.ru, chudakovdm@gmail.com
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
package com.milaboratory.mixcr.assembler;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.milaboratory.core.alignment.BandedAlignerParameters;
import com.milaboratory.mixcr.reference.GeneFeature;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE)
public final class VJCClonalAlignerParameters extends AbstractClonalAlignerParameters<VJCClonalAlignerParameters>
        implements java.io.Serializable {
    BandedAlignerParameters alignmentParameters;

    @JsonCreator
    public VJCClonalAlignerParameters(
            @JsonProperty("featureToAlign") GeneFeature featureToAlign,
            @JsonProperty("relativeMinScore") float relativeMinScore,
            @JsonProperty("alignmentParameters") BandedAlignerParameters alignmentParameters) {
        super(featureToAlign, relativeMinScore);
        this.alignmentParameters = alignmentParameters;
    }

    public BandedAlignerParameters getAlignmentParameters() {
        return alignmentParameters;
    }

    public VJCClonalAlignerParameters setAlignmentParameters(BandedAlignerParameters alignmentParameters) {
        this.alignmentParameters = alignmentParameters;
        return this;
    }

    @Override
    public VJCClonalAlignerParameters clone() {
        return new VJCClonalAlignerParameters(featureToAlign, relativeMinScore, alignmentParameters.clone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        VJCClonalAlignerParameters that = (VJCClonalAlignerParameters) o;

        if (!alignmentParameters.equals(that.alignmentParameters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + alignmentParameters.hashCode();
        return result;
    }
}