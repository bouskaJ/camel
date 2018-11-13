/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.dropbox.integration.producer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dropbox.integration.DropboxTestSupport;
import org.apache.camel.component.dropbox.util.DropboxConstants;
import org.apache.camel.component.dropbox.util.DropboxResultHeader;
import org.apache.camel.component.dropbox.util.DropboxUploadMode;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

public class DropboxProducerPutSingleFileTest extends DropboxTestSupport {

    @Test
    public void testCamelDropboxWithOptionInHeader() throws Exception {
        final Path file = Files.createTempFile("camel", ".txt");
        final Map<String, Object> headers = new HashMap<>();
        headers.put(DropboxConstants.HEADER_LOCAL_PATH, file.toAbsolutePath().toString());
        headers.put(DropboxConstants.HEADER_UPLOAD_MODE, DropboxUploadMode.add);
        template.sendBodyAndHeaders("direct:start2", null, headers);

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);
        mock.expectedHeaderReceived(DropboxResultHeader.UPLOADED_FILE.name(), workdir + "/newFile.txt");
        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start2")
                        .to("dropbox://put?accessToken={{accessToken}}&remotePath=" + workdir + "/newFile.txt")
                    .to("mock:result");
            }
        };
    }
}
