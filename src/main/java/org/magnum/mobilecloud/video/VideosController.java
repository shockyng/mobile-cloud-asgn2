/*
 *
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.magnum.mobilecloud.video;

import org.magnum.mobilecloud.video.repository.Video;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import retrofit.http.Query;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.magnum.mobilecloud.video.client.VideoSvcApi.*;

@Controller
public class VideosController {

    /**
     * You will need to create one or more Spring controllers to fulfill the
     * requirements of the assignment. If you use this file, please rename it
     * to something other than "AnEmptyController"
     * <p>
     * <p>
     * ________  ________  ________  ________          ___       ___  ___  ________  ___  __
     * |\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \
     * \ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_
     * \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \
     * \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \
     * \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
     * \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
     *
     *
     */

    private Map<Long, Video> videos = new HashMap<Long, Video>();
    private static final AtomicLong currentId = new AtomicLong(0L);

    @RequestMapping(value = "/go", method = RequestMethod.GET)
    public @ResponseBody String goodLuck() {
        return "Good Luck!";
    }

    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Video> addVideo(@RequestBody Video video) {
        checkAndSetId(video);
        videos.put(video.getId(), video);
        return new ResponseEntity<>(video, HttpStatus.OK);
    }

    @RequestMapping(value = VIDEO_SVC_PATH, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Collection<Video>> getVideoList() {
        return new ResponseEntity<>(videos.values(), HttpStatus.OK);
    }

    @RequestMapping(value = VIDEO_SVC_PATH + "/{id}/like", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Void> likeVideo(@PathVariable("id") Long id, Principal p) {
        Video video = validateVideoIsAlreadyLiked(p, id);
        video.setLikes(video.getLikes() + 1);
        video.getLikedBy().add(p.getName());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = VIDEO_SVC_PATH + "/{id}/unlike", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Void> unlikeVideo(@PathVariable("id") Long id, Principal p) {
        Video video = getVideoByIdOrThrowException(id);
        video.setLikes(video.getLikes() - 1);
        video.getLikedBy().remove(p.getName());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(value = VIDEO_SVC_PATH + "/{id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Video> getVideoById(@PathVariable("id") Long id) {
        return new ResponseEntity<>(getVideoByIdOrThrowException(id), HttpStatus.OK);
    }

    @RequestMapping(value = VIDEO_TITLE_SEARCH_PATH, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<Video>> findByTitle(@Query(TITLE_PARAMETER) String title) {
        return new ResponseEntity<>(
                videos.values()
                        .stream()
                        .filter(video -> video.getName().contains(title))
                        .collect(Collectors.toList()),
                HttpStatus.OK
        );
    }

    @RequestMapping(value = VIDEO_DURATION_SEARCH_PATH, method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<List<Video>> findByDurationLessThan(@Query(DURATION_PARAMETER) long duration) {
        return new ResponseEntity<>(
                videos.values()
                        .stream()
                        .filter(video -> video.getDuration() < duration)
                        .collect(Collectors.toList()),
                HttpStatus.OK
        );
    }

    private Video validateVideoIsAlreadyLiked(Principal principal, long id) {
        Video video = getVideoByIdOrThrowException(id);

        if (video.getLikedBy().contains(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        return video;
    }

    private Video getVideoByIdOrThrowException(long id) {
        Video video = videos.get(id);
        if (video == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        return video;
    }

    private void checkAndSetId(Video entity) {
        if (entity.getId() == 0) {
            entity.setId(currentId.incrementAndGet());
        }
    }

}
