SELECT author_name,
       description,
       guid,
       received,
       feedId,
       id,
       deleted,
       starred,
       read,
       title,
       link_href
FROM news
WHERE deleted = 0;
