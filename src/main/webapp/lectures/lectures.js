// Copyright 2020 Google LLC
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

const ENDPOINT_LECTURE_LIST = '/lecture-list';

/* Used to gather URL parameters. */
const PARAM_ID = 'id';
const PARAM_VIDEO_ID = 'video-id';

const REDIRECT_PAGE = '/view';

loadLectureList();

/**
 * Fetches avaiable Lectures from {@code ENDPOINT_LECTURE_LIST}
 * and sets them in the lecture selection page.
 */
async function loadLectureList() {
  const response = await fetch(ENDPOINT_LECTURE_LIST);
  const jsonData = await response.json();

  const lectureList = document.getElementById('lecture-list');
  lectureList.innerHTML = '';
  for (const lecture of jsonData) {
    lectureList.appendChild(createLectureListItem(lecture));
  }
}

/**
 * Creates and returns a <li> containing an <a> linking to {@code
 * lecture}'s view page.
 */
function createLectureListItem(lecture) {
  const lectureLink = document.createElement('a');

  const url = new URL(REDIRECT_PAGE, window.location.origin);
  url.searchParams.append(PARAM_ID, lecture.key.id);
  lectureLink.href = url;
  lectureLink.className = 'list-group-item list-group-item-action';

  lectureLink.innerText = lecture.lectureName;

  return lectureLink;
}
