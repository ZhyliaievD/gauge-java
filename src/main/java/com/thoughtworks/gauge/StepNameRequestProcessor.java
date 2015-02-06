// Copyright 2015 ThoughtWorks, Inc.

// This file is part of Gauge-Java.

// Gauge-Java is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Gauge-Java is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Gauge-Java.  If not, see <http://www.gnu.org/licenses/>.

package com.thoughtworks.gauge;

import gauge.messages.Messages;

import java.util.List;

public class StepNameRequestProcessor implements IMessageProcessor {
    @Override
    public Messages.Message process(Messages.Message message) {
        List<String> stepAnnotations = StepRegistry.getStepAnnotationFor(StepRegistry.getAliasStepTexts(message.getStepNameRequest().getStepValue()));
        boolean hasAlias = false, isStepPresent = false;
        if (stepAnnotations.size() > 1) hasAlias = true;
        if (stepAnnotations.size() >= 1) isStepPresent = true;
        return Messages.Message.newBuilder()
                .setMessageId(message.getMessageId())
                .setMessageType(Messages.Message.MessageType.StepNameResponse)
                .setStepNameResponse(Messages.StepNameResponse.newBuilder().addAllStepName(stepAnnotations).setIsStepPresent(isStepPresent).setHasAlias(hasAlias).build())
                .build();
    }
}
