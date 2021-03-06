// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googleinterns.zoomtube.servlets;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googleinterns.zoomtube.data.TranscriptLine;
import com.googleinterns.zoomtube.utils.LectureUtil;
import com.googleinterns.zoomtube.utils.TranscriptLineUtil;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public final class TranscriptServletTest {
  @Rule public final MockitoRule mockito = MockitoJUnit.rule();

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private TranscriptServlet transcriptServlet;
  private DatastoreService datastore;
  private StringWriter lectureTranscript;

  private static final LocalDatastoreServiceTestConfig datastoreConfig =
      (new LocalDatastoreServiceTestConfig()).setNoStorage(true);
  private static final LocalServiceTestHelper localServiceHelper =
      new LocalServiceTestHelper(datastoreConfig);
  private static final Long LECTURE_ID_A = 123L;
  private static final Long LECTURE_ID_B = 345L;
  private static final Long LECTURE_ID_C = 234L;
  // TODO: Find a way to reprsent this differently.
  private static final String SHORT_VIDEO_JSON =
      "[{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":1},\"lectureKey\":{\"kind\":\"Lect"
      + "ure\",\"id\":123},\"startTimestampMs\":400,\"durationMs\":1000,\"end"
      + "TimestampMs\":1400,\"content\":\" \"},{\"transcriptKey\":{\"kind\":\"Transcrip"
      + "tLine\",\"id\":2},\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"startTimestampMs"
      + "\":2280,\"durationMs\":1000,\"endTimestampMs\":3280,\"content"
      + "\":\"Hi\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":3},\"lectureKey\":{\"k"
      + "ind\":\"Lecture\",\"id\":123},\"startTimestampMs\":5040,\"durationMs"
      + "\":1600,\"endTimestampMs\":6640,\"content\":\"Okay\"}]";
  private static final String LONG_VIDEO_JSON =
      "[{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":1},\"lectureKey\":{\"kind\":\"Lect"
      + "ure\",\"id\":123},\"startTimestampMs\":1300,\"durationMs\":3100,\"en"
      + "dTimestampMs\":4400,\"content\":\"All right, so here we are\\nin front of the "
      + "elephants,\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":2},\"lectureKey\":{"
      + "\"kind\":\"Lecture\",\"id\":123},\"startTimestampMs\":4400,\"durationMs"
      + "\":4766,\"endTimestampMs\":9166,\"content\":\"the cool thing about these gu"
      + "ys\\nis that they have really,\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\""
      + ":3},\"lectureKey\":{\"kind\":\"Lecture\",\"id\":123},\"startTimestampMs\":9166"
      + ",\"durationMs\":3534,\"endTimestampMs\":12700,\"content\":\"really, "
      + "really long trunks,\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":4},\"lectu"
      + "reKey\":{\"kind\":\"Lecture\",\"id\":123},\"startTimestampMs\":12700,\"duratio"
      + "nMs\":4300,\"endTimestampMs\":17000,\"content\":\"and that\\u0027s, "
      + "that\\u0027s cool.\"},{\"transcriptKey\":{\"kind\":\"TranscriptLine\",\"id\":5},\"lectur"
      + "eKey\":{\"kind\":\"Lecture\",\"id\":123},\"startTimestampMs\":17000,\"duration"
      + "Ms\":1767,\"endTimestampMs\":18767,\"content\":\"And that\\u0027s pr"
      + "etty much all there is to say.\"}]";

  private static List<TranscriptLine> shortVideoTranscriptLines;
  private static List<TranscriptLine> longVideoTranscriptLines;
  private static Key lectureKeyA;
  private static Key lectureKeyB;

  @BeforeClass
  public static void createTranscriptLineLists() {
    shortVideoTranscriptLines = transcriptLines(SHORT_VIDEO_JSON);
    longVideoTranscriptLines = transcriptLines(LONG_VIDEO_JSON);
  }

  @Before
  public void setUp() throws ServletException, IOException {
    localServiceHelper.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
    transcriptServlet = new TranscriptServlet();
    transcriptServlet.init();
    lectureTranscript = new StringWriter();
    PrintWriter writer = new PrintWriter(lectureTranscript);
    when(response.getWriter()).thenReturn(writer);
    lectureKeyA = KeyFactory.createKey(LectureUtil.KIND, LECTURE_ID_A);
    lectureKeyB = KeyFactory.createKey(LectureUtil.KIND, LECTURE_ID_B);
  }

  @After
  public void tearDown() {
    localServiceHelper.tearDown();
  }

  @Test
  public void doGet_getDataInDatastoreForShortVideo() throws ServletException, IOException {
    putTranscriptLinesInDatastore(shortVideoTranscriptLines, lectureKeyA);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A.toString());

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = shortVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(expectedTranscriptLines.size());
  }

  @Test
  public void doGet_returnsLectureForLongVideoFromDatastore() throws ServletException, IOException {
    putTranscriptLinesInDatastore(longVideoTranscriptLines, lectureKeyA);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A.toString());

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = longVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(expectedTranscriptLines.size());
  }

  @Test
  public void doGet_onlyOtherLecturesInDatastore_GetNoLectures()
      throws ServletException, IOException {
    putTranscriptLinesInDatastore(shortVideoTranscriptLines, lectureKeyB);
    putTranscriptLinesInDatastore(longVideoTranscriptLines, lectureKeyA);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_C.toString());

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(0);
  }

  @Test
  public void doGet_twoLecturesInDatastore_returnsOneLecture()
      throws ServletException, IOException {
    putTranscriptLinesInDatastore(shortVideoTranscriptLines, lectureKeyB);
    putTranscriptLinesInDatastore(longVideoTranscriptLines, lectureKeyA);
    when(request.getParameter(LectureUtil.ID)).thenReturn(LECTURE_ID_A.toString());

    transcriptServlet.doGet(request, response);

    List<TranscriptLine> expectedTranscriptLines = longVideoTranscriptLines;
    List<TranscriptLine> actualTranscriptLines = transcriptLines(lectureTranscript.toString());
    assertThat(actualTranscriptLines.size()).isEqualTo(expectedTranscriptLines.size());
  }

  private static List<TranscriptLine> transcriptLines(String transcriptLinesJson) {
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY).create();
    return (ArrayList<TranscriptLine>) gson.fromJson(
        transcriptLinesJson, (new ArrayList<List<TranscriptLine>>().getClass()));
  }

  private void putTranscriptLinesInDatastore(List<TranscriptLine> transcriptLines, Key lectureKey) {
    for (int i = 0; i < transcriptLines.size(); i++) {
      Entity lineEntity = TranscriptLineUtil.createEntity(lectureKey, "test content",
          /* start= */ 0, /* duration= */ 0, /* end= */ 0);
      datastore.put(lineEntity);
    }
  }

  private int entitiesInDatastoreCount(long lectureId) {
    // A limit of 100 for the maximum number of entities counted is used because
    // we can assume that for this test datastore, there won't be more than 100 entities
    // for a lecture key.
    return datastore.prepare(filteredQueryOfTranscriptLinesByLectureId(lectureId))
        .countEntities(withLimit(100));
  }

  private Query filteredQueryOfTranscriptLinesByLectureId(long lectureId) {
    Key lectureKey = KeyFactory.createKey(LectureUtil.KIND, lectureId);
    Filter lectureFilter =
        new FilterPredicate(TranscriptLineUtil.LECTURE, FilterOperator.EQUAL, lectureKey);
    return new Query(TranscriptLineUtil.KIND).setFilter(lectureFilter);
  }
}
