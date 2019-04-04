package com.sp.bbs;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sp.common.FileManager;
import com.sp.common.MyUtil;
import com.sp.member.SessionInfo;

@Controller("bbs.boardController")
public class BoardController {
	@Autowired
	private BoardService boardService;
	@Autowired
	private MyUtil myUtil;
	@Autowired
	private FileManager fileManager;
	
	
	
	@RequestMapping(value="/bbs/list")
	public String list(
			@RequestParam(value="page", defaultValue="1") int current_page,
			@RequestParam(defaultValue="subject") String condition,
			@RequestParam(defaultValue="") String keyword,
			@RequestParam(value="rows", defaultValue="10", required=false) int rows,
			HttpServletRequest req,
			Model model) throws Exception{
		// 키값 인코딩하기
		if(req.getMethod().equalsIgnoreCase("GET")) {
			keyword = URLDecoder.decode(keyword, "UTF-8");			
		}
		
		// 검색키, 값 받기
		Map<String, Object> map= new HashMap<>();
		map.put("condition", condition);
		map.put("keyword", keyword);
		
		// 전체 데이터 개수, 페이지 수 구하기
		int total_page=0;
		int dataCount=0;
		dataCount=boardService.dataCount(map);
		if(dataCount!=0)
			total_page=myUtil.pageCount(rows, dataCount);
		
		if(current_page>total_page)
			current_page=total_page;
		
		// 테이블에서 가져올 시작과 끝 위치 구하기
		int start=(current_page-1)*rows+1;
		int end=current_page*rows;
		map.put("start", start);
		map.put("end", end);	
		
		// 테이블에서 게시물 리스트 가져오기
		List<Board> list=boardService.listBoard(map);
		
		int listNum;
		int n=0;
		for(Board dto:list) {
			listNum = dataCount - (start+n-1);
			n++;
			dto.setListNum(listNum);
		}
		
		//listUrl과 articleURL 설정
		String cp=req.getContextPath();		
		String query="";
		String listUrl = cp + "/bbs/list";
		String articleUrl = cp+ "/bbs/article?page="+current_page;
		if(keyword.length()!=0) {
			query="condition="+condition+"&keyword="+
					URLEncoder.encode(keyword, "UTF-8");
			listUrl += "?"+query;
			articleUrl += "&"+query;
		}
		// 페이징 처리
		String paging=myUtil.paging(current_page, total_page, listUrl);
		
		// 포워딩할 jsp에 넘길 값 설정
		model.addAttribute("list", list);
		model.addAttribute("dataCount", dataCount);
		model.addAttribute("page", current_page);
		model.addAttribute("total_page", total_page);
		model.addAttribute("paging", paging);
		model.addAttribute("articleUrl", articleUrl);
		model.addAttribute("condition", condition);
		model.addAttribute("keyword", keyword);		
		
		return ".bbs.list";
	}
	
	@RequestMapping(value="/bbs/created",method=RequestMethod.GET)
	public String createdForm(
			Model model) throws Exception{
		
		model.addAttribute("mode", "created");
		return ".bbs.created";
	}
	
	@RequestMapping(value="/bbs/created",method=RequestMethod.POST)
	public String createdSubmit(
			HttpSession session,
			HttpServletRequest req,
			Board dto
			) throws Exception{
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		
		String root = session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"bbs";	//파일을 저장할 경로
		
		dto.setUserId(info.getUserId());
		dto.setIpAddr(req.getRemoteAddr());
		boardService.insertBoard(dto, pathname);
		
		return "redirect:/bbs/list";
	}
	@RequestMapping(value="/bbs/article")
	public String article(
			@RequestParam int num,
			@RequestParam(defaultValue="subject") String condition,
			@RequestParam(defaultValue="") String keyword,
			@RequestParam(defaultValue="1") int page,
			HttpSession session,
			Model model
			) throws Exception{
		
		//검색 값 디코딩
		keyword=URLDecoder.decode(keyword, "UTF-8");
		
		// 이전글, 다음글, 리스트에서 사용할 파라미터
		String query="page="+page;
		if(keyword.length() != 0) {
			query+="condition="+condition+"&keyword="+
					URLEncoder.encode(keyword, "UTF-8");
		}
		// 조회수 증가
		boardService.updateHitCount(num);
		
		//게시글 가져오기
		Board dto=boardService.readBoard(num);
		
		//게시글이 없으면 리스트로 리다이렉트
		if(dto==null)
			return "redirect:/bbs/list?"+query;
		
		// 글 내용 엔터등을 <br>로 변경
		dto.setContent(myUtil.htmlSymbols(dto.getContent()));
		
		Map<String, Object> map=new HashMap<>();
		map.put("num", num);
		map.put("condition", condition);
		map.put("keyword", keyword);
		
		
		String root = session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"bbs";	//파일을 저장할 경로
		dto.setFilesize(fileManager.getFilesize(pathname));
		
		// 이전글 가져오기
		Board preReadDTO=boardService.preReadBoard(map);
		// 다음글 가져오기
		Board nextReadDTO=boardService.nextReadBoard(map);
		
		// 포워딩할 JSP에 넘길 데이터 (dto, 이전글, 다음글, query, 페이지)
		model.addAttribute("dto", dto);
		model.addAttribute("preReadDTO", preReadDTO);
		model.addAttribute("nextReadDTO", nextReadDTO);
		model.addAttribute("page", page);
		model.addAttribute("query", query);
		
		return ".bbs.article";
	}	
	
	@RequestMapping(value="/bbs/download")
	public void download(
			@RequestParam int num,
			HttpServletRequest req,
			HttpServletResponse resp,
			HttpSession session
			) throws Exception {

		String root=session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"bbs";
		
		Board dto=boardService.readBoard(num);
		if(dto!=null) {
			boolean b=fileManager.doFileDownload(dto.getSaveFilename(),
					dto.getOriginalFilename(), pathname, resp);
			if(b) return;
		}
		
		resp.setContentType("text/html;charset=utf-8");
		PrintWriter out=resp.getWriter();
		out.print("<script>alert('파일 다운로드를 실패했습니다.');history.back;</script>");	
	}
	
	@RequestMapping(value="/bbs/update", method=RequestMethod.GET)
	public String updateForm(
			@RequestParam int num,
			@RequestParam String page,
			HttpSession session,
			Model model ) throws Exception{
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		
		Board dto = boardService.readBoard(num);
		if(dto==null)
			return "redirect:/bbs/list?page="+page;

		if(! info.getUserId().equals(dto.getUserId()))
			return "redirect:/bbs/list?page="+page;
		
		model.addAttribute("dto", dto);
		model.addAttribute("page", page);
		model.addAttribute("mode", "update");

		return ".bbs.created";
	}
	
	@RequestMapping(value="/bbs/update", method=RequestMethod.POST)
	public String updateSubmit(
			@RequestParam String page,
			HttpSession session,
			Board dto) {
		
		String root=session.getServletContext().getRealPath("/");
		String pathname= root+"uploads"+File.separator+"bbs";
		
		//수정하기
		boardService.updateBoard(dto, pathname);
		
		return "redirect:/bbs/list?page="+page;
	}
	
	@RequestMapping(value="/bbs/deleteFile")
	public String deleteFile(
			@RequestParam int num,
			@RequestParam String page,
			HttpSession session
			) throws Exception {
		SessionInfo info=(SessionInfo)session.getAttribute("member");

		String root=session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"bbs";
		
		Board dto=boardService.readBoard(num);
		if(dto==null)
			return "redirect:/bbs/list?page="+page;
		
		if(! info.getUserId().equals(dto.getUserId()))
			return "redirect:/bbs/list?page="+page;
		
		if(dto.getSaveFilename()!=null) {
			fileManager.doFileDelete(dto.getSaveFilename(), pathname);
			dto.setSaveFilename("");
			dto.setOriginalFilename("");
			boardService.updateBoard(dto, pathname);	//DB테이블의 파일명 변경
		}
		 
		return "redirect:/bbs/list?page="+page;
	}
	
	@RequestMapping(value="/bbs/delete")
	public String delete(
			@RequestParam int num,
			@RequestParam String page,
			HttpSession session) throws Exception {
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		
		String root=session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"bbs";
		boardService.deleteBoard(num, pathname, info.getUserId());
		
		return "redirect:/bbs/list?page="+page;
	}
	
	@RequestMapping(value="/bbs/insertReply", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertReply(
			HttpSession session,
			Reply dto){
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		dto.setUserId(info.getUserId());
		
		int result=boardService.insertReply(dto);
		String state="true";
		if(result==0) {
			state="false";
		}
		
		Map<String, Object> model=new HashMap<>();
		model.put("state", state);
		return model;
	}
	
	@RequestMapping(value="/bbs/listReply")
	public String listReply(
			@RequestParam int num,
			@RequestParam(value="pageNo", defaultValue="1") int current_page,
			Model model
			) throws Exception{
		//AJAX-text/html
		// 테이블에서 가져올 시작과 끝 값 구하기
		int rows=5;
		int dataCount=0;
		int total_page=0;
		Map<String, Object> map=new HashMap<>();
		map.put("num", num);
		
		dataCount = boardService.replyCount(map);
		total_page= myUtil.pageCount(rows, dataCount);
		if(current_page > total_page)
			current_page=total_page;
		
		int start=(current_page-1)*rows+1;
		int end=current_page*rows;
		map.put("start", start);
		map.put("end", end);
		
		List<Reply> listReply=boardService.listReply(map);
		for(Reply dto : listReply) {
			dto.setContent(myUtil.htmlSymbols(dto.getContent()));
		}
		
		String paging=myUtil.pagingMethod(current_page, total_page, "listPage");
		
		model.addAttribute("listReply", listReply);
		model.addAttribute("pageNo", current_page);
		model.addAttribute("replyCount", dataCount );
		model.addAttribute("total_page", total_page);
		model.addAttribute("paging", paging);
		
		return "bbs/listReply";
	}
	
	@RequestMapping(value="/bbs/listReplyAnswer", method=RequestMethod.GET)
	public String listReplyAnswer(
			@RequestParam int answer,
			Model model){
		List<Reply> listReplyAnswer=boardService.listReplyAnswer(answer);;
		for(Reply dto:listReplyAnswer) {
			dto.setContent(myUtil.htmlSymbols(dto.getContent()));
		}
		model.addAttribute("listReplyAnswer", listReplyAnswer);
		
		return "bbs/listReplyAnswer";
	}
	
	@RequestMapping(value="/bbs/countReplyAnswer", method=RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> countReplyAnswer(
			@RequestParam int answer) throws Exception {
		int count=boardService.replyAnswerCount(answer);
		
		Map<String, Object> model = new HashMap<>();
		model.put("count", count);
		
		return model;
	}
	
	@RequestMapping(value="/bbs/boardLike", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> boardLike(
			@RequestParam int num,
			HttpSession session
			) throws Exception {
		// 게시글 좋아요 추가 및 게시글 좋아요 개수 - AJAX : JSON
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		Map<String, Object> map=new HashMap<>();
		map.put("num", num);
		map.put("userId", info.getUserId());
		
		int result=boardService.insertBoardLike(map);
		
		String state="true";
		if(result==0)
			state="false";
		
		int count=boardService.boardLikeCount(num);
		
		Map<String, Object> model=new HashMap<>();
		model.put("state", state);
		model.put("count", count);
	
		return model;
	}
	
	@RequestMapping(value="/bbs/insertReplyLike", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> insertReplyLike(
			@RequestParam Map<String, Object> paramMap,
			HttpSession session
			) throws Exception {
		// 댓글 좋아요/싫어요 등록 및 개수 가져오기 - AJAX : JSON
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		paramMap.put("userId", info.getUserId());
		
		int result=boardService.insertReplyLike(paramMap);
		
		String state="true";
		if(result==0)
			state="false";
		
		
		Map<String, Object> countMap=boardService.replyLikeCount(paramMap);
		// 마이바티스에서 resultType이 map인 경우 int는 BigDecimal로 넘어온다.
		int likeCount=((BigDecimal)countMap.get("LIKECOUNT")).intValue();
		int disLikeCount=((BigDecimal)countMap.get("DISLIKECOUNT")).intValue();
		
		Map<String, Object> model=new HashMap<>();
		model.put("state", state);
		model.put("likeCount", likeCount);
		model.put("disLikeCount", disLikeCount);
	
		return model;
	}
	
	@RequestMapping(value="/bbs/deleteReply", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteReplyLike(
			@RequestParam Map<String, Object> paramMap
			) throws Exception {
		//댓글 및 답글 삭제 : AJAX-JSON
		int result=boardService.deleteReply(paramMap);
		String state="true";
		
		if(result==0)
			state="false";
		
		Map<String, Object> model=new HashMap<>();
		model.put("state", state);
		return model;
	}
}
