<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%
   String cp = request.getContextPath();
%>

<script type="text/javascript">
// 전역변수
var pageNo = 1;
var condition = "all";
var keyword = "";

function ajaxHTML(url, type, query, id){
	$.ajax({
		type:type,
		url:url,
		data:query,
		success:function(data){
			$("#"+id).html(data);
		},
		beforeSend:function(e){
			e.setRequestHeader("AJAX", true);
		},
		error:function(e){
			if(e.status==403){
				location.href="<%=cp%>/member/login"
				return;
			}
			console.log(e.responseText);
		}
	});
}

function ajaxJSON(url, type, query, mode){
	$.ajax({
		type:type,
		url:url,
		data:query,
		dataType:"json",
		success:function(data){
			if(mode=="delete"){
				if(data.state=="false"){
					alert("삭제 권한이 없습니다.");
				} else {
					listPage(pageNo);
				}
			}
		},
		beforeSend:function(e){
			e.setRequestHeader("AJAX", true);
		},
		error:function(e){
			if(e.status==403){
				location.href="<%=cp%>/member/login"
				return;
			}
			console.log(e.responseText);
		}
	});
}

function ajaxFileJSON(url, query, mode){
	//파일 업로드를 위한 ajax() 함수
	$.ajax({
		type:"post",
		url:url,
		processData:false,	// file 전송시 필수 (데이터를 쿼리문자열로 변환 여부)
		contentType:false,	// file 전송시 필수 (인코딩 형식 사용여부)
		data:query,
		dataType:"json",
		success:function(data){
			listPage(pageNo);
		},
		beforeSend:function(e){
			e.setRequestHeader("AJAX", true);
		},
		error:function(e){
			if(e.status==403){
				location.href="<%=cp%>/member/login"
				return;
			}
			console.log(e.responseText);
		}
	});
}
function listPage(page){
	//페이징
	pageNo = page;
	
	var id="board-body";
	var url="<%=cp%>/abbs/list";
	var query="pageNo="+page;
	if(keyword!=""){
		query+="&condition="+condition+"&keyword="+encodeURIComponent(keyword);
	}
	ajaxHTML(url, "get", query, id);
	
}
$(function(){
	listPage(1);
});

// list.jsp 에서 쓰는 함수
function reloadBoard(){
	// 새로고침
	
}
function searchList(){
	condition=$("#condition").val();
	keyword=$("#keyword").val();
	
	listPage(1);
}

function insertBoard(){
	// 글쓰기 폼
	var url="<%=cp%>/abbs/created";
	$("#board-body").load(url);	// AJAX-GET
}

function articleBoard(num){
	// 글보기
	var id="board-body";
	var url="<%=cp%>/abbs/article";
	var query="num="+num+"&pageNo="+pageNo;
	if(keyword!=""){
		query+="&condition="+condition+"&keyword="+encodeURIComponent(keyword);
	}
	
	ajaxHTML(url, "get", query, id);
	
}
// created.jsp에서 쓰는 함수
function sendBoard(mode){
	// 글 등록 완료 또는 수정 완료
	var f=document.boardForm;
	
	if(! f.subject.value){
		f.subject.focus();
		return;
	}
	if(! f.content.value){
		f.content.focus();
		return;
	}
	
	if(mode=='created'){
		condition="all";
		keyword="";
		pageNo=1;
	}
	
	var url="<%=cp%>/abbs/"+mode;
	var query=new FormData(f);	// 파일첨부. FormData 객체 : jquery.form.js에 존재
	// var query=$("form[name=boardForm]").serialize()	//파일 첨부 불가
	
	ajaxFileJSON(url, query, mode);
}
// article.jsp에서 쓰이는 함수
function updateBoard(num){
	// 글 수정 폼
	var url="<%=cp%>/abbs/update";
	var query="num="+num+"&pageNo="+pageNo;
	if(keyword!=""){
		query+="&condition="+condition+"&keyword="+encodeURIComponent(keyword);
	}
	
	ajaxHTML(url, "get", query, "board-body");
	
	
}
function deleteBoard(num){
	// 글 삭제 완료
	if(! confirm("삭제하시겠습니까?")){
		return;
	}
	var url="<%=cp%>/abbs/delete";
	var query="num="+num;
	ajaxJSON(url, "post", query, "delete");
}

$(function(){
	$("body").on("click", "#deleteBoardFile", function() {
		var num=$(this).attr("data-num");
		var $td=$(this).closest("td");
		var url="<%=cp%>/abbs/deleteFile";
		var query="num="+num;
		
		$.ajax({
			type:"post",
			url:url,
			data:query,
			dataType:"json",
			success:function(data){
				if(data.state=="true"){
					var f=document.boardForm;
					f.saveFilename.value="";
					f.originalFilename.value="";
					$td.empty();
				}
			},
			beforeSend:function(e){
				e.setRequestHeader("AJAX", true);
			},
			error:function(e){
				if(e.status==403){
					location.href="<%=cp%>/member/login"
					return;
				}
				console.log(e.responseText);
			}
		});		
	});
});

</script>

<div class="body-container" style="width: 700px;">
	<div class="body-title">
		<h3><span style="font-family: Webdings">2</span> AJAX-게시판 </h3>
	</div>
	
	<div id="board-body">
	
	
	</div>
</div>