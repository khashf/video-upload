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
package org.magnum.dataup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.multipart.MultipartFile;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;

@Controller
public class VideoServiceController  {
	
	private static final AtomicLong currentId = new AtomicLong(0L);
	private Map<Long, Video> videos = new HashMap<Long, Video>();
	private VideoFileManager videoDataMgr;
	
	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideoMetaData(@RequestBody Video v) {
		// Set video's id
		checkAndSetId(v);
		// Set video's data url
		v.setDataUrl(getDataUrl(v.getId()));
		// Add video to Map
		//videos.put(v.getId(), v);
		videos.put(v.getId(), v);
		return v;
	}
	
	private void checkAndSetId(Video v) {
		if (v.getId() == 0) {
			v.setId(currentId.incrementAndGet());
		}
	}
	
	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}
	
	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String base = 
				"http://" 
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? (":" + request.getServerPort()) : "");
		return base;
	}
	
	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos.values();
	}
	
	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable("id") long id,
			@RequestParam("data") MultipartFile videoData,
			HttpServletResponse response) throws Exception {
		
		VideoStatus videoStatus = new VideoStatus(VideoStatus.VideoState.PROCESSING);
		try {
			videoDataMgr = VideoFileManager.get();
		    videoDataMgr.saveVideoData(videos.get(id), videoData.getInputStream());
			videoStatus.setState(VideoStatus.VideoState.READY);
			response.setStatus(200);
			videoData.getInputStream().close();
			return videoStatus;
		}
		catch (Exception e) {
			response.sendError(404);
			response.setStatus(404);
			return videoStatus;
		}
		
	}
	
	
	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public @ResponseBody void getData(
			@PathVariable("id") long id,
			HttpServletResponse response) throws Exception {
		videoDataMgr = VideoFileManager.get();
		
		try {
			videoDataMgr.copyVideoData(videos.get(id), response.getOutputStream());
			response.setStatus(200);
		}
		catch (Exception e) {
			response.setStatus(404);
		}
		
	}
}
