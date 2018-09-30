/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.system;

import static io.zeebe.broker.test.EmbeddedBrokerConfigurator.setPartitionCount;
import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.broker.test.EmbeddedBrokerRule;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.test.broker.protocol.clientapi.ClientApiRule;
import io.zeebe.test.broker.protocol.clientapi.ExecuteCommandResponse;
import io.zeebe.test.util.TestUtil;
import io.zeebe.test.util.record.RecordingExporter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class UniqueKeyFormatTest {

  public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule(setPartitionCount(3));
  public ClientApiRule apiRule = new ClientApiRule(brokerRule::getClientAddress);

  @Rule public RuleChain ruleChain = RuleChain.outerRule(brokerRule).around(apiRule);

  @Test
  public void shouldStartWorkflowInstanceAtNoneStartEvent() {
    // given
    apiRule
        .partition()
        .deploy(Bpmn.createExecutableProcess("process").startEvent("foo").endEvent().done());

    // when
    TestUtil.waitUntil(() -> RecordingExporter.deploymentRecords().withPartitionId(2).exists());
    final ExecuteCommandResponse workflowInstanceWithResponse =
        apiRule.partition(2).createWorkflowInstanceWithResponse("process");

    // then partition id is encoded in the returned key
    final long key = workflowInstanceWithResponse.key();
    final int partitionId = (int) (key >> SystemConstants.KEYSPACE_POW_OF_2);
    assertThat(partitionId).isEqualTo(2);
  }
}