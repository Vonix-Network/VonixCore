import React, { useState, useEffect } from 'react';
import MarkdownRenderer from '../components/MarkdownRenderer';

const Documentation = ({ file }) => {
    const [content, setContent] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        setLoading(true);
        fetch(`/docs/${file}`)
            .then(res => res.text())
            .then(text => {
                setContent(text);
                setLoading(false);
            })
            .catch(err => {
                console.error(err);
                setContent('# Error loading documentation\nCould not load ' + file);
                setLoading(false);
            });
    }, [file]);

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[50vh]">
                <div className="w-8 h-8 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin" style={{ borderColor: 'var(--accent-primary)', borderTopColor: 'transparent' }}></div>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto pb-20">
            <MarkdownRenderer content={content} />
        </div>
    );
};

export default Documentation;
