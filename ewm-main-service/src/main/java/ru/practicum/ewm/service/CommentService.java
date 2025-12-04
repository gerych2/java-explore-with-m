package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserService userService;
    private final EventService eventService;

    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        User author = userService.getUserById(userId);
        Event event = eventService.getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }

        Comment comment = CommentMapper.toEntity(dto, event, author);
        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created: id={}, eventId={}, authorId={}", savedComment.getId(), eventId, userId);
        return CommentMapper.toDto(savedComment);
    }

    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto) {
        userService.getUserById(userId);
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        if (dto.getText() != null) {
            comment.setText(dto.getText());
            comment.setEdited(LocalDateTime.now());
        }

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment updated: id={}", commentId);
        return CommentMapper.toDto(savedComment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        userService.getUserById(userId);
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));

        commentRepository.delete(comment);
        log.info("Comment deleted: id={}", commentId);
    }

    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        eventService.getEventById(eventId);
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> comments = commentRepository.findByEventId(eventId, pageable);
        return comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    public CommentDto getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
        return CommentMapper.toDto(comment);
    }

    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> comments = commentRepository.findByAuthorId(userId, pageable);
        return comments.stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }
}

