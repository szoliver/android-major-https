/**
 * Android Jungle-Major-Https framework project.
 *
 * Copyright 2016 Arno Zhang <zyfgood12@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jungle.majorhttps.request.upload;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.jungle.majorhttps.request.base.BizBaseRequest;
import com.jungle.majorhttps.request.base.BizRequestListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BizMultipartRequest extends BizBaseRequest<String> {

    private static final String UPLOAD_CONTENT_TYPE = "multipart/form-data";
    private static final String UPLOAD_BOUNDARY = "biz-upload-request-";


    private List<MultipartFormItem> mFormItems;


    public BizMultipartRequest(
            int seqId, int method, String url, List<MultipartFormItem> list,
            Map<String, String> headers,
            BizRequestListener<String> listener) {

        super(seqId, method, url, null, headers, listener);

        mFormItems = list;
        setShouldCache(false);
    }

    @Override
    public String getBodyContentType() {
        return String.format("%s; boundary=%s", UPLOAD_CONTENT_TYPE, UPLOAD_BOUNDARY);
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        if (mFormItems == null || mFormItems.isEmpty()) {
            return super.getBody();
        }

        int size = 0;
        for (MultipartFormItem item : mFormItems) {
            byte[] content = item.getFormContent();
            size += content != null ? content.length : 0;
        }

        //
        // Write Format:
        //
        // --boundary
        // Content-Disposition: form-data; name="files0"; filename="item_file_name_0"
        // Content-Type: application/octet-stream
        // Content-Transfer-Encoding: binary
        //
        // item_file_content_0
        //
        // --boundary
        // Content-Disposition: form-data; name="files1"; filename="item_file_name_1"
        // Content-Type: application/octet-stream
        // Content-Transfer-Encoding: binary
        //
        // item_file_content_1
        //
        // --boundary--
        //

        int index = 0;
        ByteArrayOutputStream stream = new ByteArrayOutputStream(size);
        for (MultipartFormItem item : mFormItems) {
            byte[] content = item.getFormContent();
            if (content == null) {
                continue;
            }

            StringBuilder buffer = new StringBuilder();
            buffer.append("--").append(UPLOAD_BOUNDARY).append("\r\n");
            buffer.append("Content-Disposition: form-data;")
                    .append(" name=\"").append("files").append(index).append("\";")
                    .append(" filename=\"").append(item.getFormName()).append("\"\r\n");

            buffer.append("Content-Type: ").append(item.getMimeType()).append("\r\n");
            buffer.append("Content-Transfer-Encoding: binary\r\n\r\n");

            try {
                stream.write(buffer.toString().getBytes("utf-8"));
                stream.write(content);
                stream.write("\r\n".getBytes("utf-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            ++index;
        }

        try {
            stream.write(String.format("--%s--\r\n", UPLOAD_BOUNDARY).getBytes("utf-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream.toByteArray();
    }

    @Override
    protected String parseResponseContent(NetworkResponse response) {
        return parseResponseToStringContent(response);
    }
}
