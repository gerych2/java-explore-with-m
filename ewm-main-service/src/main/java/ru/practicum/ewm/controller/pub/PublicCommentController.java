package ru.practicum.ewm.controller.pub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.service.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getEventComments(@PathVariable Long eventId,
                                             @RequestParam(defaultValue = "0") int from,
                                             @RequestParam(defaultValue = "10") int size) {
        log.info("Getting event comments: eventId={}, from={}, size={}", eventId, from, size);
        return commentService.getEventComments(eventId, from, size);
    }

    @GetMapping("/{commentId}")
    public CommentDto getComment(@PathVariable Long eventId,
                                 @PathVariable Long commentId) {
        log.info("Getting comment: eventId={}, commentId={}", eventId, commentId);
        return commentService.getComment(commentId);
    }
}

