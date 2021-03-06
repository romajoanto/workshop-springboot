package org.desarrolladorslp.workshops.springboot.services.impl;

import java.util.List;
import java.util.Objects;

import javax.persistence.EntityNotFoundException;

import org.desarrolladorslp.workshops.springboot.forms.BoardForm;
import org.desarrolladorslp.workshops.springboot.models.Board;
import org.desarrolladorslp.workshops.springboot.models.Card;
import org.desarrolladorslp.workshops.springboot.models.Column;
import org.desarrolladorslp.workshops.springboot.models.User;
import org.desarrolladorslp.workshops.springboot.repository.BoardRepository;
import org.desarrolladorslp.workshops.springboot.repository.CardRepository;
import org.desarrolladorslp.workshops.springboot.repository.ColumnRepository;
import org.desarrolladorslp.workshops.springboot.repository.UserRepository;
import org.desarrolladorslp.workshops.springboot.services.BoardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardServiceImpl implements BoardService {

    private BoardRepository boardRepository;
    private UserRepository userRepository;
    private ColumnRepository columnRepository;
    private CardRepository cardRepository;

    public BoardServiceImpl(BoardRepository boardRepository,
                            UserRepository userRepository,
                            ColumnRepository columnRepository,
                            CardRepository cardRepository) {
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public Board create(BoardForm boardForm) {
        if (Objects.isNull(boardForm.getUserId())) {
            throw new IllegalArgumentException("User id required");
        }
        User user = userRepository.findById(boardForm.getUserId()).orElseThrow(() -> new EntityNotFoundException("User not found"));

        Board newBoard = new Board();
        newBoard.setName(boardForm.getName());
        newBoard.setUser(user);

        return boardRepository.save(newBoard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Board> findByUser(Long userId) {

        if (Objects.isNull(userId)) {
            throw new IllegalArgumentException("User id required");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        return boardRepository.findBoardsByUser(user);
    }

    @Override
    public Board findById(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Board not found"));
    }

    @Override
    public Board findByIdAndUserId(Long id, Long userId) {
        return boardRepository.findByIdAndUserId(id, userId).orElseThrow(
                () -> new EntityNotFoundException("Board for User not found"));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Board not found"));
        boardRepository.delete(board);
    }

    @Override
    public Board update(BoardForm boardForm) {
        Board toUpdate = findById(boardForm.getId());
        toUpdate.setName(boardForm.getName());
        return boardRepository.save(toUpdate);
    }

    @Override
    @Transactional
    public Board duplicate(Long id) {

        Board existentBoard = findById(id);
        Board duplicatedBoard = new Board();

        duplicatedBoard.setName("COPY " + existentBoard.getName());
        duplicatedBoard.setUser(existentBoard.getUser());
        duplicatedBoard.setId(null);

        boardRepository.save(duplicatedBoard);

        List<Column> columns = columnRepository.findColumnsByBoard(existentBoard);

        columns.forEach(column -> {
            Column duplicatedColumn = new Column();

            duplicatedColumn.setBoard(duplicatedBoard);
            duplicatedColumn.setName(column.getName());

            columnRepository.save(duplicatedColumn);

            cardRepository.findByColumn(column.getId()).forEach(card -> {
                Card duplicatedCard = new Card();

                duplicatedCard.setColumn(duplicatedColumn);
                duplicatedCard.setDescription(card.getDescription());

                cardRepository.save(duplicatedCard);
            });
        });

        return duplicatedBoard;
    }
}
