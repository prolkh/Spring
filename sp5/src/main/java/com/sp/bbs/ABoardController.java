package com.sp.bbs;

import java.io.File;
import java.io.PrintWriter;
import java.net.URLDecoder;
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

@Controller("bbs.aBoardController")
public class ABoardController {
	@Autowired
	private BoardService boardService;
	
	@Autowired
	private MyUtil myUtil;
	
	@Autowired
	private FileManager fileManager;
	
	@RequestMapping(value="/abbs")
	public String main() {
		return ".abbs.main";
	}
	
	@RequestMapping(value="/abbs/list")
	public String list(
			@RequestParam(value="pageNo", defaultValue="1") int current_page,
			@RequestParam(defaultValue="subject") String condition,
			@RequestParam(defaultValue="") String keyword,
			HttpServletRequest req,
			Model model) throws Exception{
		// 페이지 리스트 - AJAX : TEXT
		
		// 키값 인코딩하기
		if(req.getMethod().equalsIgnoreCase("GET")) {
			keyword = URLDecoder.decode(keyword, "UTF-8");			
		}
		
		// 검색키, 값 받기
		Map<String, Object> map= new HashMap<>();
		map.put("condition", condition);
		map.put("keyword", keyword);
		
		// 전체 데이터 개수, 페이지 수 구하기
		int rows=10;
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
		
		//listUrl과 articleURL 설정	- AJAX는 필요 없음

		// 페이징 처리
		String paging=myUtil.pagingMethod(current_page, total_page, "listPage");
		
		// 포워딩할 jsp에 넘길 값 설정
		model.addAttribute("list", list);
		model.addAttribute("dataCount", dataCount);
		model.addAttribute("page", current_page);
		model.addAttribute("total_page", total_page);
		model.addAttribute("paging", paging);
		
		model.addAttribute("condition", condition);
		model.addAttribute("keyword", keyword);		
		
		return "abbs/list";
	}
	
	@RequestMapping(value="/abbs/created",method=RequestMethod.GET)
	public String createdForm(
			Model model) throws Exception{
		// 글쓰기 폼 AJAX : TEXT
		model.addAttribute("mode", "created");
		return "abbs/created";
	}
	
	@RequestMapping(value="/abbs/created",method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> createdSubmit(
			HttpServletRequest req,
			HttpSession session,
			Board dto
			) throws Exception{
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		
		String root = session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"abbs";	//파일을 저장할 경로
		
		dto.setUserId(info.getUserId());
		dto.setIpAddr(req.getRemoteAddr());
		
		String state="true";
		int result=boardService.insertBoard(dto, pathname);
		if(result==0)
			state="false";
		
		Map<String, Object> model = new HashMap<>();
		model.put("state", state);
		
		return model;
	}
	
	@RequestMapping(value="/abbs/article")
	public String article(
			@RequestParam int num,
			@RequestParam(defaultValue="1") int pageNo,
			@RequestParam(defaultValue="subject") String condition,
			@RequestParam(defaultValue="") String keyword,
			HttpSession session,
			HttpServletRequest req,
			Model model
			) throws Exception{
		// 글보기 AJAX:TEXT
		String root = session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"abbs";	//파일을 저장할 경로
		
		// 조회수 증가
		boardService.updateHitCount(num);
		
		//게시글 가져오기
		Board dto=boardService.readBoard(num);
		//게시글이 없으면 리스트로 리다이렉트
		if(dto==null) {
			return list(pageNo, condition, keyword, req, model);
		}

		
		// 글 내용 엔터등을 <br>로 변경
		dto.setContent(myUtil.htmlSymbols(dto.getContent()));
		dto.setFilesize(fileManager.getFilesize(pathname));
		
		Map<String, Object> map=new HashMap<>();
		map.put("num", num);
		map.put("condition", condition);
		keyword = URLDecoder.decode(keyword, "utf-8");
		map.put("keyword", keyword);
		
		// 이전글 가져오기
		Board preReadDTO=boardService.preReadBoard(map);
		// 다음글 가져오기
		Board nextReadDTO=boardService.nextReadBoard(map);
		
		// 포워딩할 JSP에 넘길 데이터 (dto, 이전글, 다음글, query, 페이지)
		model.addAttribute("dto", dto);
		model.addAttribute("preReadDTO", preReadDTO);
		model.addAttribute("nextReadDTO", nextReadDTO);
		
		return "abbs/article";
	}	
	@RequestMapping(value="/abbs/delete", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> delete(
			@RequestParam int num,
			HttpSession session) throws Exception {
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		
		String root=session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"abbs";
		int result = boardService.deleteBoard(num, pathname, info.getUserId());
		
		String state="true";
		if(result==0)
			state="false";
		
		Map<String, Object> model=new HashMap<>();
		model.put("state",  state);
		
		return model;
	}
	
	@RequestMapping(value="/abbs/update", method=RequestMethod.GET)
	public String updateForm(
			@RequestParam int num,
			@RequestParam(defaultValue="1") int pageNo,
			@RequestParam(defaultValue="all") String condition,
			@RequestParam(defaultValue="") String keyword,
			HttpSession session,
			HttpServletRequest req,
			Model model ) throws Exception{
		// 글 수정 폼 - AJAX:TEXT
		SessionInfo info=(SessionInfo)session.getAttribute("member");
		
		Board dto = boardService.readBoard(num);
		if(dto==null)
			return list(pageNo, condition, keyword, req, model);

		if(! info.getUserId().equals(dto.getUserId()))
			return list(pageNo, condition, keyword, req, model);
		
		model.addAttribute("dto", dto);
		model.addAttribute("mode", "update");

		return "abbs/created";
	}
	
	@RequestMapping(value="/abbs/update", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> updateSubmit(
			HttpSession session,
			Board dto) {
		// 글 수정 완료 - AJAX:JSON
		String root=session.getServletContext().getRealPath("/");
		String pathname= root+"uploads"+File.separator+"abbs";
		
		//수정하기
		boardService.updateBoard(dto, pathname);
		
		String state="true";
		
		Map<String, Object> model=new HashMap<>();
		model.put("state", state);
		
		return model;
	}


	@RequestMapping(value="/abbs/download")
	public void download(
			@RequestParam int num,
			HttpServletRequest req,
			HttpServletResponse resp,
			HttpSession session
			) throws Exception {

		String root=session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"abbs";
		
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
	
	@RequestMapping(value="/abbs/deleteFile", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> deleteFile(
			@RequestParam int num,
			HttpSession session
			) throws Exception {
		SessionInfo info=(SessionInfo)session.getAttribute("member");

		String root=session.getServletContext().getRealPath("/");
		String pathname=root+"uploads"+File.separator+"abbs";
		
		
		String state="true";
		Map<String, Object> model = new HashMap<>();
		Board dto=boardService.readBoard(num);
		if(dto==null) {
			state="false";
			model.put("state", state);
			return model;
		}
		
		if(! info.getUserId().equals(dto.getUserId())) {
			state="false";
			model.put("state", state);
			return model;
		}
		
		if(dto.getSaveFilename()!=null) {
			fileManager.doFileDelete(dto.getSaveFilename(), pathname);
			dto.setSaveFilename("");
			dto.setOriginalFilename("");
			boardService.updateBoard(dto, pathname);	//DB테이블의 파일명 변경
		}
		 
		model.put("state", state);
		return model;
	}
}
